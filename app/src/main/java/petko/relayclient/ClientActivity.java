package petko.relayclient;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;
import relay.petko.relaymain.RelayService;

public class ClientActivity extends AppCompatActivity {

    private boolean relayServiceBound = false;
    private RelayService relayService;
    private ServiceConnection relayServiceConnection;

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
                if(relayServiceBound){
                    relayService.transmit(new byte[]{(byte)0xA0,(byte)0x01,(byte)0x01,(byte)0xA2});
                }
            }
        });
        findViewById(R.id.realyKi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(relayServiceBound){
                    relayService.transmit(new byte[]{(byte)0xA0,(byte)0x01,(byte)0x00,(byte)0xA1});
                }
            }
        });
        new registerTask().execute();
    }

    @Override
    protected void onStart() {
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (relayServiceBound) {
            unbindService(relayServiceConnection);
            relayServiceBound = false;
        }
    }

    @SuppressLint("StaticFieldLeak")
    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    private class registerTask extends AsyncTask<Void, Void, List<String>> {

        TextView t;
        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            t = (TextView) findViewById(R.id.textView);
            t.setText("");
            progress = new ProgressDialog(ClientActivity.this);
            progress.setTitle("Töltés");
            progress.setMessage("Adatok kérése a Szervertől.");
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();
        }

        @SuppressLint("ShowToast")
        @Override
        protected void onPostExecute(List<String> result) {
            progress.dismiss();
            if(!result.isEmpty()) {
                for (String s : result) {
                    log.info(s);
                    t.append(s);
                    String[] data = s.split("[:]");
                    @SuppressLint("CommitPrefEdits")
                    SharedPreferences.Editor editor = RelayClientApplication.config.edit();
                    editor.putString(RelayClientApplication.PreferenceKeys.DEVICE_ID, data[1]);
                }
            }else{
                Toast.makeText(ClientActivity.this,"Nem sikerült kapcsolatot létesíteni!",Toast.LENGTH_SHORT);
            }
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> res = new ArrayList<>();
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://192.168.0.107:9000/Register");
            try {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                String ip = Formatter.formatIpAddress(wm != null ? wm.getConnectionInfo().getIpAddress() : 0);
                String deviceType = "controller";

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

            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }

    }
}
