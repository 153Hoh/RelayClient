package relay.petko.relaymain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;


public class RelayReceiver extends BroadcastReceiver {

    private static final CustomLogger log = Logging.getLogger(RelayReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        log.debug(intent);
        if (action != null) {
            IBinder binder = peekService(context, new Intent(context, RelayService.class));
            if (binder != null) {
                UsbDevice usbDevice;
                RelayService relayService = ((RelayService.RelayBinder) binder).getService();
                switch (action) {
                    case RelayService.USB_PERMISSION:
                        usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (usbDevice != null) {
                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                log.debug("Usb permission successfully granted for " + usbDevice.getDeviceName());
                                relayService.open(usbDevice);
                            } else {
                                log.warn("Usb permission denied for " + usbDevice.getDeviceName());
                            }
                        }
                        break;
                    case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                        usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (usbDevice != null) {
                            relayService.open(usbDevice);
                        }
                        break;
                    case UsbManager.ACTION_USB_DEVICE_DETACHED:
                        usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (usbDevice != null) {
                            relayService.close(usbDevice);
                        }
                        break;
                }
            }
        }

    }
}
