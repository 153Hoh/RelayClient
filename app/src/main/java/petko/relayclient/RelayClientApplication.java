package petko.relayclient;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


public class RelayClientApplication extends Application {

    public static SharedPreferences config;

    @Override
    public void onCreate() {
        super.onCreate();
        config = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static final class PreferenceKeys {

        public static final String DEVICE_NAME = "deviceName";
        public static final String DEVICE_ID = "deviceId";
        public static final String DEVICE_TYPE = "devieType";
        public static final String SERVER_ADDRESS = "serverAddress";
    }

    public static final class ClientActions {

    }
}
