package relay.petko.relay.log;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LogLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomLogger {

    private static final String DEFAULT_TAG = "uninitialized";
    private static final Map<String, ServerLogListener> logListeners = new HashMap<>();
    private final Logger log;

    CustomLogger(Logger log) {
        this.log = log;
    }

    public static synchronized String addServerLogListener(final ServerLogListener listener) {
        String id = UUID.randomUUID().toString();
        logListeners.put(id, listener);
        return id;
    }

    public static synchronized void removeServerLogListener(final String id) {
        logListeners.remove(id);
    }

    public void trace(Object message) {
        if (message instanceof Throwable) {
            trace("", (Throwable) message);
        } else {
            if (Logging.initialized) {
                log.trace(message);
            } else {
                android.util.Log.v(DEFAULT_TAG, message != null ? message.toString() : "null");
            }
        }
    }

    void trace(Object message, Throwable t) {
        if (Logging.initialized) {
            log.trace(message, t);
        } else {
            android.util.Log.v(DEFAULT_TAG, message != null ? message.toString() : "null", t);
        }
    }

    public void debug(Object message) {
        if (message instanceof Throwable) {
            debug("", (Throwable) message);
        } else {
            if (Logging.initialized) {
                log.debug(message);
            } else {
                android.util.Log.d(DEFAULT_TAG, message != null ? message.toString() : "null");
            }
        }
    }

    void debug(Object message, Throwable t) {
        if (Logging.initialized) {
            log.debug(message, t);
        } else {
            android.util.Log.d(DEFAULT_TAG, message != null ? message.toString() : "null", t);
        }
    }

    public void debugServer(Object message) {
        debug(message);
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.DEBUG, message, null);
        }
    }

    public void debugServer(Object message, Throwable t) {
        debug(message, t);
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.DEBUG, message, t);
        }
    }

    public void info(Object message) {
        if (message instanceof Throwable) {
            info("", (Throwable) message);
        } else {
            if (Logging.initialized) {
                log.info(message);
            } else {
                android.util.Log.i(DEFAULT_TAG, message != null ? message.toString() : "null");
            }
        }
    }

    void info(Object message, Throwable t) {
        if (Logging.initialized) {
            log.info(message, t);
        } else {
            android.util.Log.i(DEFAULT_TAG, message != null ? message.toString() : "null", t);
        }
    }

    public void infoServer(Object message) {
        info(message);
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.INFO, message, null);
        }
    }

    public void infoServer(Object message, Throwable t) {
        info(message, t);
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.INFO, message, t);
        }
    }

    public void warn(Object message) {
        if (message instanceof Throwable) {
            warn("", (Throwable) message);
        } else {
            if (Logging.initialized) {
                log.warn(message);
            } else {
                android.util.Log.w(DEFAULT_TAG, message != null ? message.toString() : "null");
            }
        }
    }

    public void warn(Object message, Throwable t) {
        if (Logging.initialized) {
            log.warn(message, t);
        } else {
            android.util.Log.w(DEFAULT_TAG, message != null ? message.toString() : "null", t);
        }
    }

    public void warnServer(Object message) {
        warn(message);
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.WARNING, message, null);
        }
    }

    public void warnServer(Object message, Throwable t) {
        warn(message, t);
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.WARNING, message, t);
        }
    }

    public void error(Object message) {
        error(message, true);
    }

    public void error(Object message, Throwable t) {
        error(message, t, true);
    }

    public void error(Object message, boolean doServerLog) {
        if (message instanceof Throwable) {
            error("", (Throwable) message, doServerLog);
        } else {
            if (Logging.initialized) {
                log.error(message);
            } else {
                android.util.Log.e(DEFAULT_TAG, message != null ? message.toString() : "null");
            }
            if (doServerLog) {
                for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
                    entry.getValue().onServerLog(LogLevel.ERROR, message, null);
                }
            }
        }
    }

    public void error(Object message, Throwable t, boolean doServerLog) {
        if (Logging.initialized) {
            log.error(message, t);
        } else {
            android.util.Log.e(DEFAULT_TAG, message != null ? message.toString() : "null", t);
        }
        if (doServerLog) {
            for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
                entry.getValue().onServerLog(LogLevel.ERROR, message, t);
            }
        }
    }

    public void fatal(Object message) {
        if (message instanceof Throwable) {
            fatal("", (Throwable) message);
        } else {
            if (Logging.initialized) {
                log.fatal(message);
            } else {
                android.util.Log.wtf(DEFAULT_TAG, message != null ? message.toString() : "null");
            }
            for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
                entry.getValue().onServerLog(LogLevel.FATAL, message, null);
            }
        }
    }

    public void fatal(Object message, Throwable t) {
        if (Logging.initialized) {
            log.fatal(message, t);
        } else {
            android.util.Log.wtf(DEFAULT_TAG, message != null ? message.toString() : "null", t);
        }
        for (Map.Entry<String, ServerLogListener> entry : CustomLogger.logListeners.entrySet()) {
            entry.getValue().onServerLog(LogLevel.FATAL, message, t);
        }
    }

}
