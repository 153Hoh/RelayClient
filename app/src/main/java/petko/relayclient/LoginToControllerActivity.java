package petko.relayclient;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;
import relay.petko.relay.utils.RegisterCallback;

public class LoginToControllerActivity extends AppCompatActivity implements RegisterCallback {

    private static final CustomLogger log = Logging.getLogger(LoginToControllerActivity.class);

    EditText nameText;
    EditText passText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_to_controller);
        nameText = findViewById(R.id.NameText);
        passText = findViewById(R.id.PasswordText);
        findViewById(R.id.LoginButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();
                String pass = passText.getText().toString();
                log.debug(name + ":" + pass);
                new LoginTask(name,pass).execute();
            }
        });
    }

    @Override
    public void onRegister(String s) {
        if(s.equalsIgnoreCase("done")){
            Intent intent = new Intent(LoginToControllerActivity.this,ControllerActivity.class);
            startActivity(intent);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoginTask extends AsyncTask<Void,Void,List<String>>{

        private String name;
        private String pass;
        boolean err = false;
        ProgressDialog progress;

        private LoginTask(String name, String pass) {
            this.name = name;
            this.pass = pass;
        }

        private void callReg(RegisterCallback registerCallback, String message){
            registerCallback.onRegister(message);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progress = new ProgressDialog(LoginToControllerActivity.this);
            progress.setTitle("Bejelentkezés...");
            progress.setMessage("Bejelentkezés a Szerverre.");
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected void onPostExecute(List<String> res){
            progress.dismiss();
            if(err){
                Toast.makeText(LoginToControllerActivity.this,"Sikertelen bejelentkezés!",Toast.LENGTH_SHORT).show();
            } else if (!res.isEmpty()){
                callReg(LoginToControllerActivity.this,res.get(0));
            } else {
                Toast.makeText(LoginToControllerActivity.this,"Hiba történt!",Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> res = new ArrayList<>();
            final HttpParams httpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
            HttpClient client = new DefaultHttpClient(httpParams);
            HttpPost post = new HttpPost(RelayClientApplication.config.getString(RelayClientApplication.PreferenceKeys.SERVER_ADDRESS, null) + "/Register");
            try {

                List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                nameValuePairs.add(new BasicNameValuePair("registerCont","registerCont"));
                nameValuePairs.add(new BasicNameValuePair("name", name));
                nameValuePairs.add(new BasicNameValuePair("pass", pass));

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
