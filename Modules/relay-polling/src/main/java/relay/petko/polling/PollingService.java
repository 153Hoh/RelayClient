package relay.petko.polling;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;

public class PollingService extends Service{

    private static final CustomLogger log = Logging.getLogger(PollingService.class);

    public static final String HAVE_DATA = BuildConfig.APPLICATION_ID + ".HAVE_DATA";

    private final IBinder binder = new PollingBinder();
    private HandlerThread handlerThread;
    private Handler handler;
    private final LocalBroadcastManager localBroadcastManager;

    public PollingService(final Context context) {
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public void onCreate(){
        log.debug("onCreate-begin");
        super.onCreate();
        handlerThread = new HandlerThread("PollingService HandlerThread");
        handler =  new Handler(handlerThread.getLooper());
        log.debug("onCreate-end");

    }

    @Override
    public void onDestroy() {
        log.debug("onDestroy-Begin");
        handlerThread.quit();

        super.onDestroy();
        log.debug("onDestroy-End");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log.debug("OnBind");
        return binder;
    }

    public class PollingBinder extends Binder {

        public PollingService getService() {
            return PollingService.this;
        }

    }

    private class PollingRunnable implements Runnable{

        @Override
        public void run() {
            List<String> res = new ArrayList<>();
            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(30 * 1000).build();
            HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            HttpPost post = new HttpPost("http://192.168.0.107:9000/Pollget");
            try {

                List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                nameValuePairs.add(new BasicNameValuePair("poll", "poll"));

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
            if(!res.isEmpty()){
                localBroadcastManager.sendBroadcast(new Intent(HAVE_DATA).putStringArrayListExtra("data", (ArrayList<String>) res));
            }
        }
    }
}
