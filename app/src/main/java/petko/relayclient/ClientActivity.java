package petko.relayclient;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;
import relay.petko.relaymain.RelayService;

public class ClientActivity extends AppCompatActivity implements RegisterCallback{

    private boolean relayServiceBound = false;
    private RelayService relayService;
    private ServiceConnection relayServiceConnection;
    private List<String> dataFromServer = new ArrayList<>();
    private BroadcastReceiver getDataReceiver;
    private RegisterTask registerTask;
    private String deviceId;
    boolean registerDone = false;

    public static final String HAVE_DATA = BuildConfig.APPLICATION_ID + ".HAVE_DATA";

    private static final CustomLogger log = Logging.getLogger(ClientActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView t = (TextView) findViewById(R.id.textView);
        t.setText("");
        findViewById(R.id.relayBe).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View view) {
                log.debug(RelayClientApplication.PreferenceKeys.SERVER_ADDRESS);
                if (relayServiceBound) {
                    relayService.transmit(new byte[]{(byte) 0xA0, (byte) 0x01, (byte) 0x01, (byte) 0xA2});
                }
            }
        });
        findViewById(R.id.realyKi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (relayServiceBound) {
                    relayService.transmit(new byte[]{(byte) 0xA0, (byte) 0x01, (byte) 0x00, (byte) 0xA1});
                }
            }
        });
        registerTask = new RegisterTask();
        registerTask.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        log.debug("onResume-begin");
        log.debug("onResume-end");
    }

    @Override
    protected void onStart() {
        log.debug("onStart-begin");
        super.onStart();
        relayServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                RelayService.RelayBinder binder = (RelayService.RelayBinder) service;
                relayService = binder.getService();
                relayServiceBound = true;
                relayService.init();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (relayServiceBound) {
                    unbindService(relayServiceConnection);
                    relayServiceBound = false;
                }
            }
        };
        Intent intent = new Intent(this, RelayService.class);
        bindService(intent, relayServiceConnection, Context.BIND_AUTO_CREATE);

        getDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case HAVE_DATA:
                            dataFromServer = intent.getStringArrayListExtra("data");
                            for (String s : dataFromServer) {
                                log.debug(s);
                            }
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(HAVE_DATA);
        registerReceiver(getDataReceiver, filter);
        log.debug("onStart-end");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(getDataReceiver);
        if (relayServiceBound) {
            unbindService(relayServiceConnection);
            relayServiceBound = false;
        }
    }

    @Override
    public void onRegister(String deviceId) {
        log.debug("itten" + deviceId);
        @SuppressLint("CommitPrefEdits")
        SharedPreferences.Editor editor = RelayClientApplication.config.edit();
        editor.putString(RelayClientApplication.PreferenceKeys.DEVICE_ID, deviceId);
        editor.apply();
        log.debug("batd " + RelayClientApplication.config.getString(RelayClientApplication.PreferenceKeys.DEVICE_ID,null));
    }

    @SuppressLint("StaticFieldLeak")
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private class RegisterTask extends AsyncTask<Void, Void, List<String>> {

        TextView t;
        boolean err = false;
        ProgressDialog progress;

        private void callReg(RegisterCallback registerCallback, String deviceName){
            registerCallback.onRegister(deviceName);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            t = (TextView) findViewById(R.id.textView);
            t.setText("");

            progress = new ProgressDialog(ClientActivity.this);
            progress.setTitle("Regisztráció...");
            progress.setMessage("Eszköz regisztrálása.");
            progress.setCancelable(false);
            progress.show();
        }

        @SuppressLint("ShowToast")
        @Override
        protected void onPostExecute(List<String> result) {
            if (!result.isEmpty()) {
                for (String s : result) {
                    log.info(s);
                    t.append(s);
                    String[] data = s.split("[:]");
                    callReg(ClientActivity.this,data[1]);
                }
            } else if (err) {
                Toast.makeText(getApplicationContext(), "Nem sikerült kapcsolatot létesíteni!", Toast.LENGTH_LONG).show();
            }
            progress.dismiss();
            registerDone = true;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> res = new ArrayList<>();
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
            HttpClient client = new DefaultHttpClient(httpParams);
            HttpPost post = new HttpPost(BuildConfig.SERVER_ADDRESS + "/Register"); // CSERÉLD!!
            try {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                String ip = Formatter.formatIpAddress(wm != null ? wm.getConnectionInfo().getIpAddress() : 0);
                String deviceType = RelayClientApplication.config.getString(RelayClientApplication.PreferenceKeys.DEVICE_TYPE,null);

                List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                nameValuePairs.add(new BasicNameValuePair("register", "register"));
                nameValuePairs.add(new BasicNameValuePair("ip", ip));
                nameValuePairs.add(new BasicNameValuePair("devicetype", deviceType));

                post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = client.execute(post);
                BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    while ((line = rd.readLine()) != null) {
                        log.info(line);
                        res.add(line);
                    }
                } else {
                    log.warn("Hiba történt!");
                }

            } catch (ConnectTimeoutException e) {
                log.debug("asd");
                err = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }
    }
}
