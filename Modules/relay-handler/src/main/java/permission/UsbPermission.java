package permission;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.IBinder;

import android.hardware.usb.IUsbManager;
import android.os.ServiceManager;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;

public class UsbPermission {

    private static final CustomLogger log = Logging.getLogger(UsbPermission.class);

    @SuppressLint("ObsoleteSdkInt")
    static public boolean grantDevicePermission(final Context context, final UsbDevice usbDevice) {

        try {

            ApplicationInfo applicationInfo = context.getApplicationInfo();
            if (applicationInfo == null) {
                log.warn("ApplicationInfo couldn't be null!");
                return false;
            }

            final int uid = applicationInfo.uid;

            IBinder iBinder = ServiceManager.getService(Context.USB_SERVICE);
            IUsbManager iUsbManager = IUsbManager.Stub.asInterface(iBinder);

            if (iUsbManager == null) {
                log.warn("UsbManager interface couldn't be null!");
                return false;
            }

            log.debug("Trying to get permission for device " + usbDevice.getDeviceName());
            if (Build.VERSION.SDK_INT >= 17) {

                iUsbManager.grantDevicePermission(usbDevice, uid);
                int userId = uid / 100000;
                iUsbManager.setDevicePackage(usbDevice, context.getPackageName(), userId);

                return true;
            } else if (Build.VERSION.SDK_INT >= 14) {

                iUsbManager.grantDevicePermission(usbDevice, uid);
                iUsbManager.setDevicePackage(usbDevice, context.getPackageName());

                return true;
            } else {
                log.warn("'android.permission.MANAGE_USB permission confirmation dialog' is not supported for API: " + Build.VERSION.SDK_INT);
            }

        } catch (SecurityException e) {
            log.warn("SecurityException occurred, cannot bypass USB permission dialog." +
                    " Application must be a system application, installed to " + (Build.VERSION.SDK_INT < 19 ? "/system/app" : "/system/priv-app") + "…");
        } catch (Exception e) {
            log.warn(e);
        }

        return false;
    }

    @SuppressWarnings("unused")
    public static void requestPermission(final Context context, final UsbDevice usbDevice, final PendingIntent permissionIntent) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        usbManager.requestPermission(usbDevice, permissionIntent);
    }

}
