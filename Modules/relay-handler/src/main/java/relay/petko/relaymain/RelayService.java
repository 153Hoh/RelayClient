package relay.petko.relaymain;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import converter.CH341;
import permission.UsbPermission;
import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;


public class RelayService extends Service {

    public static final String USB_PERMISSION = BuildConfig.APPLICATION_ID + ".USB_PERMISSION";

    private static final CustomLogger log = Logging.getLogger(RelayService.class);

    public static boolean isRunning = false;
    public static RelayService instance = null;
    private static boolean initialized = false;
    private static PendingIntent usbPermissionIntent;
    private static UsbManager usbManager;
    private static CH341 serialConverter;
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private static JSONObject commandSet;
    private final IBinder binder = new RelayBinder();
    private HandlerThread handlerThread;
    private Handler handler;
    private CH341.SerialListener serialListener = new CH341.SerialListener() {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onDataReceived(byte[] data) {
            log.info("Data received from serial: " + new String(data, StandardCharsets.UTF_8));
        }
    };
    private Runnable initRunnable = new Runnable() {

        @Override
        public void run() {
            if (initialized) {

                log.debug("Relay Service already initialized.");

            } else {

                log.debug("Initializing Relay Service…");

                usbPermissionIntent = PendingIntent.getBroadcast(RelayService.this, 0, new Intent(USB_PERMISSION), 0);
                usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                serialConverter = new CH341(usbManager);

                final UsbDevice usbDevice = findSupportedUsbDevice();
                if (usbDevice == null) {
                    log.debug("Supported USB-Serial converter not found!");
                    initialized = true;
                    return;
                }

                initialized = true;

                openDevice(usbDevice);

            }

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log.info("onBind");
        return binder;
    }

    @Override
    public void onCreate() {
        log.debug("onCreate-Begin");
        instance = this;
        isRunning = true;

        super.onCreate();

        handlerThread = new HandlerThread("RelayService HandlerThread");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());
        log.debug("onCreate-End");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        log.debug("onDestroy-Begin");
        destroy();
        handlerThread.quit();
        isRunning = false;
        instance = null;

        super.onDestroy();
        log.debug("onDestroy-End");
    }

    public void init() {
        handler.post(initRunnable);
    }

    public void open(@NonNull UsbDevice usbDevice) {
        handler.post(new OpenDeviceRunnable(usbDevice));
    }

    @SuppressWarnings("unused")
    public void loadCommandSet(InputStream inputStream) {
        handler.post(new LoadCommandSetRunnable(inputStream));
    }

    public void transmit(final byte[] message) {
        handler.post(new TransmitRunnable(message));
    }

    public void close(@NonNull UsbDevice usbDevice) {
        handler.post(new CloseDeviceRunnable(usbDevice));
    }

    public void destroy() {
        handler.post(new CloseDeviceRunnable(serialConverter.getDevice()));
    }

    private UsbDevice findSupportedUsbDevice() {
        for (final UsbDevice usbDevice : usbManager.getDeviceList().values()) {
            if (serialConverter.isSupported(usbDevice)) {
                log.debug("Supported USB-Serial converter found: " + usbDevice.getDeviceName());
                return usbDevice;
            }
        }
        return null;
    }

    private void openDevice(UsbDevice usbDevice) {
        if (initialized && serialConverter != null && !serialConverter.isOpened()) {
            if (!usbManager.hasPermission(usbDevice)) {
                log.debug("Usb permission has not granted for this device yet.");
                UsbPermission.grantDevicePermission(RelayService.this, usbDevice);
            }
            if (!usbManager.hasPermission(usbDevice)) {
                log.warn("Usb permission granting could not bypass for device.");
                usbManager.requestPermission(usbDevice, usbPermissionIntent);
                return;
            }
            serialConverter.open(usbDevice);
            serialConverter.setBaudRate(9600);
            serialConverter.setListener(serialListener);
        }
    }

    private void closeDevice(UsbDevice usbDevice) {
        if (initialized && serialConverter != null && serialConverter.isOpened() &&
                serialConverter.getDevice().equals(usbDevice)) {
            serialConverter.close();
        }
    }

    public class RelayBinder extends Binder {

        public RelayService getService() {
            return RelayService.this;
        }

    }

    private class OpenDeviceRunnable implements Runnable {

        private final UsbDevice usbDevice;

        OpenDeviceRunnable(UsbDevice usbDevice) {
            this.usbDevice = usbDevice;
        }

        @Override
        public void run() {
            if (initialized && serialConverter != null) {
                if (serialConverter.isSupported(usbDevice)) {
                    openDevice(usbDevice);
                } else {
                    log.warn("Usb device is not supported.");
                }
            }
        }
    }

    private class CloseDeviceRunnable implements Runnable {

        private final UsbDevice usbDevice;

        CloseDeviceRunnable(UsbDevice usbDevice) {
            this.usbDevice = usbDevice;
        }

        @Override
        public void run() {
            closeDevice(usbDevice);
        }
    }

    private class LoadCommandSetRunnable implements Runnable {

        private final InputStream input;

        LoadCommandSetRunnable(InputStream inputStream) {
            this.input = inputStream;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                StringBuilder sb = new StringBuilder();
                String content;
                while ((content = reader.readLine()) != null) {
                    sb.append(content).append("\n");
                }
                content = sb.toString().trim();
                if (!content.isEmpty()) {
                    commandSet = new JSONObject(sb.toString().trim());
                }
            } catch (Exception e) {
                log.warn(e);
            }
        }
    }

    private class TransmitRunnable implements Runnable {

        private final byte[] message;

        TransmitRunnable(final byte[] message) {
            this.message = message;
        }

        @Override
        public void run() {
            if (initialized && serialConverter != null && serialConverter.isOpened()) {
                final int transferredBytes = serialConverter.transmit(message, message.length);
                if (transferredBytes > 0) {
                    log.info("Transmit success: " + message);
                    return;
                }
                log.warn("Transmit error: " + message);
            }
        }

    }
}
