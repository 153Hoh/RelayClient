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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;
import relay.petko.relay.utils.DataFromServerCallback;

public class PollingService extends Service{

    private static final CustomLogger log = Logging.getLogger(PollingService.class);

    public static final String HAVE_DATA = BuildConfig.APPLICATION_ID + ".HAVE_DATA";

    private final IBinder binder = new PollingBinder();
    private HandlerThread handlerThread;
    private Handler handler;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onCreate(){
        log.debug("onCreate-begin");
        super.onCreate();
        handlerThread = new HandlerThread("PollingService HandlerThread");
        handlerThread.start();
        handler =  new Handler(handlerThread.getLooper());
        log.debug("onCreate-end");

    }

    @Override
    public void onDestroy() {
        log.debug("onDestroy-Begin");
        handlerThread.quit();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(6000, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(6000, TimeUnit.SECONDS))
                {
                    log.warn("polling did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

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

    public void startPolling(String deviceId, Context context, DataFromServerCallback dataFromServerCallback){
        handler.post(new PollingRunnable(deviceId, context, dataFromServerCallback));
    }

    public class PollingBinder extends Binder {

        public PollingService getService() {
            return PollingService.this;
        }

    }

    private class PollingRunnable implements Runnable{

        private final String deviceId;
        private LocalBroadcastManager localBroadcastManager;
        private DataFromServerCallback dataFromServerCallback;

        private PollingRunnable(String deviceId, Context context, DataFromServerCallback dataFromServerCallback) {
            this.deviceId = deviceId;
            this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
            this.dataFromServerCallback = dataFromServerCallback;
        }

        private void dataReceived(DataFromServerCallback dataFromServerCallback, String from, List<String> data){
            dataFromServerCallback.onDataReceived(from, data);
        }

        @Override
        public void run() {
            log.debug("Poll-start");
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    List<String> res = new ArrayList<>();
                    HttpClient client = new DefaultHttpClient();
                    HttpPost post = new HttpPost(BuildConfig.SERVER_ADDRESS + "/Poll");
                    try {

                        List<NameValuePair> nameValuePairs = new ArrayList<>(1);
                        nameValuePairs.add(new BasicNameValuePair("id", deviceId));

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
                        log.debug("HAVE_DATA");
                        dataReceived(dataFromServerCallback,HAVE_DATA,res);
                    }
                }
            },0,5000, TimeUnit.MILLISECONDS);
        log.debug("Poll-end");
        }
    }
}
