package converter;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.Arrays;
import java.util.Locale;

import relay.petko.relay.log.CustomLogger;
import relay.petko.relay.log.Logging;

public class CH341 {

    private static final CustomLogger log = Logging.getLogger(CH341.class);

    private static final int USB_RECIP_DEVICE = 0x00;
    public static final int DEVICE_OUT_REQTYPE = UsbConstants.USB_TYPE_VENDOR | USB_RECIP_DEVICE
            | UsbConstants.USB_DIR_OUT;
    public static final int DEVICE_IN_REQTYPE = UsbConstants.USB_TYPE_VENDOR | USB_RECIP_DEVICE
            | UsbConstants.USB_DIR_IN;
    private static final int CH341_BAUDBASE_FACTOR = 1532620800;
    private static final int CH341_BAUDBASE_DIVMAX = 3;
    private static final int DEFAULT_TIMEOUT = 100;
    private static final int BUFFER_SIZE = 4096;
    private static final int[][] COMPATIBLE_DEVICES = new int[][]{{0x4348, 0x4348},
            {0x1a86, 0x7523}, {0x1a86, 0x5523}};
    private static SerialListener mListener = null;
    private UsbManager mUsbManager = null;
    private UsbDevice mUsbDevice = null;
    private UsbDeviceConnection mConnection = null;
    private UsbInterface mInterface = null;
    private UsbEndpoint mEpIn = null;
    private UsbEndpoint mEpOut = null;
    private InputThread mReadThread = null;

    public CH341(final UsbManager usbManager) {
        if (usbManager == null) {
            throw new IllegalArgumentException("UsbManager is null.");
        }

        mUsbManager = usbManager;
    }

    public static String toHexString(final byte[] buffer) {
        String bufferString = "";
        for (final byte element : buffer) {
            String hexChar = Integer.toHexString(element & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = '0' + hexChar;
            }
            bufferString += hexChar.toUpperCase(Locale.ENGLISH) + ' ';
        }
        return bufferString;
    }

    public synchronized void open(final UsbDevice usbDevice) {
        mUsbDevice = usbDevice;

        if (mUsbDevice == null) {
            throw new IllegalArgumentException("UsbDevice is null.");
        }

        if (!isSupported(mUsbDevice)) {
            throw new IllegalArgumentException("Device is not compatible.");
        }

        mConnection = mUsbManager.openDevice(mUsbDevice);
        mInterface = mUsbDevice.getInterface(0);

        if (!mConnection.claimInterface(mInterface, false)) {
            mConnection = null;
            mInterface = null;
            throw new IllegalArgumentException("Cannot claim interface.");
        }

        mEpIn = mInterface.getEndpoint(0);
        mEpOut = mInterface.getEndpoint(1);

        if ((mEpIn.getDirection() == UsbConstants.USB_DIR_OUT)
                && (mEpOut.getDirection() == UsbConstants.USB_DIR_IN)) {
            final UsbEndpoint epTemp = mEpIn;
            mEpIn = mEpOut;
            mEpOut = epTemp;
        }

        mListener = new SerialListener() {
            @Override
            public void onDataReceived(byte[] data) {

            }
        };

        mReadThread = new InputThread();
        mReadThread.start();
    }

    public synchronized void close() {
        try {
            mConnection.releaseInterface(mInterface);
            mConnection.close();
        } catch (final NullPointerException e) {
            log.warn(e);
        }

        mConnection = null;
        mUsbDevice = null;
        mInterface = null;
        mEpIn = null;
        mEpOut = null;
        mReadThread.interrupt();
        mReadThread = null;
    }

    public UsbDevice getDevice() {
        return mUsbDevice;
    }

    public boolean isOpened() {
        return mConnection != null;
    }

    public boolean isSupported(final UsbDevice device) {
        for (final int[] compatibleDevice : COMPATIBLE_DEVICES) {
            if ((compatibleDevice[0] == device.getVendorId())
                    && (compatibleDevice[1] == device.getProductId())) {
                return true;
            }
        }
        return false;
    }

    private int ch341ControlOut(final int request, final int value, final int index) {
        return mConnection.controlTransfer(DEVICE_OUT_REQTYPE, request, value, index, null, 0,
                DEFAULT_TIMEOUT);
    }

    public int setBaudRate(final int baud) {
        short a, b;
        int r;
        int factor;
        short divisor;

        factor = (CH341_BAUDBASE_FACTOR / baud);
        divisor = CH341_BAUDBASE_DIVMAX;

        while ((factor > 0xfff0) && (divisor != 0)) {
            factor >>= 3;
            divisor--;
        }

        if (factor > 0xfff0) {
            return -2;
        }

        factor = 0x10000 - factor;
        a = (short) ((factor & 0xff00) | divisor);
        b = (short) (factor & 0xff);

        r = ch341ControlOut(0x9a, 0x1312, a);
        if (r == -1) {
            r = ch341ControlOut(0x9a, 0x0f2c, b);
        }

        return r;
    }

    public int transmit(final byte[] buffer, final int bufferLength) {
        return mConnection.bulkTransfer(mEpOut, buffer, bufferLength, DEFAULT_TIMEOUT);
    }

    public synchronized SerialListener getListener() {
        return mListener;
    }

    public synchronized void setListener(SerialListener listener) {
        mListener = listener;
    }

    public interface SerialListener {
        void onDataReceived(byte[] data);
    }

    private class InputThread extends Thread {
        private final int sleepInterval = 100;

        @Override
        public void run() {
            try {
                byte[] mBuffer = new byte[BUFFER_SIZE];
                int transferredBytes;
                do {
                    transferredBytes = mConnection.bulkTransfer(mEpIn, mBuffer, BUFFER_SIZE, DEFAULT_TIMEOUT);

                    if (transferredBytes > 0) {
                        byte[] response = Arrays.copyOfRange(mBuffer, 0, transferredBytes);

                        mListener.onDataReceived(response);
                    }

                    sleep(sleepInterval);
                } while (true);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
