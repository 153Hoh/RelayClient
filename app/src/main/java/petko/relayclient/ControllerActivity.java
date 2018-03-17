package petko.relayclient;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

public class ControllerActivity extends AppCompatActivity {

    private static final CustomLogger log = Logging.getLogger(ControllerActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View view) {
            new SendDataTask("relayOn").execute();
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SendDataTask("relayOff").execute();
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private class SendDataTask extends AsyncTask<Void,Void,List<String>>{

        private String command;
        TextView t;
        boolean err = false;
        ProgressDialog progress;

        private SendDataTask(String command) {
            this.command = command;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            t = findViewById(R.id.textView7);
            t.setText("");

            progress = new ProgressDialog(ControllerActivity.this);
            progress.setTitle("Regisztráció...");
            progress.setMessage("Eszköz regisztrálása.");
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(List<String> res){
            t.setText("");
            if(!res.isEmpty()) {
                for (String s : res) {
                    t.append(s);
                }
            } else if (err) {
                Toast.makeText(ControllerActivity.this, "Sikertelen kapcsolódás!",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ControllerActivity.this, "Hiba történt!",Toast.LENGTH_SHORT).show();
            }
            progress.dismiss();
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> res = new ArrayList<>();
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
            HttpClient client = new DefaultHttpClient(httpParams);
            HttpPost post = new HttpPost(RelayClientApplication.config.getString(RelayClientApplication.PreferenceKeys.SERVER_ADDRESS, null) + "/SendCommandToDevice");
            try {

                List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                nameValuePairs.add(new BasicNameValuePair("commandDevice","commandDevice"));
                nameValuePairs.add(new BasicNameValuePair("command", command));

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
                    err = true;
                }
            } catch (ConnectTimeoutException e) {
                err = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return res;
        }
    }
}
