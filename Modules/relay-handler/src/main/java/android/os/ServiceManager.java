package android.os;

import java.util.Map;

public class ServiceManager {
    /**
     * @param name
     */
    public static IBinder getService(final String name) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Place a new @a service called @a name into the service manager.
     *
     * @param name    the name of the new service
     * @param service the service object
     */
    public static void addService(final String name, final IBinder service) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Retrieve an existing service called @a name from the service manager.
     * Non-blocking.
     *
     * @param name
     */
    public static IBinder checkService(final String name) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @throws RemoteException
     */
    public static String[] listServices() throws RemoteException {
        throw new RuntimeException("Stub!");
    }

    /**
     * This is only intended to be called when the process is first being
     * brought up and bound by the activity manager. There is only one thread in
     * the process at that time, so no locking is done.
     *
     * @param cache the cache of service references
     * @hide
     */
    public static void initServiceCache(final Map<String, IBinder> cache) {
        throw new RuntimeException("Stub!");
    }

}
