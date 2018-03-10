package relay.petko.relay.log;

import android.support.annotation.NonNull;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class Logging {

    private static final Map<String, StatusAppender> appenderList = new HashMap<>();
    public static File logFile;
    static boolean initialized = false;
    private static Level logLevel = Level.ALL;
    private static final CustomLogger log = Logging.getLogger(Logging.class);

    public static CustomLogger getLogger(Class<?> cls) {
        init();
        return new CustomLogger(Logger.getLogger(cls));
    }

    public static void init() {
        if (!initialized) {
            try {
                Logging.configureLog();
            } catch (Exception e) {
                log.warn("Error while configuring file logging.");
            }
        }
    }

    private static void configureLog() {
        if (logFile != null) {
            if (!logFile.exists()) {
                try {
                    if (!logFile.createNewFile()) {
                        throw new IOException("creating new file unsuccessful");
                    }
                } catch (final IOException e) {
                    String message = e.getMessage();
                    if (message != null) {
                        log.error("Failed to create logfile: " + message);
                    } else {
                        log.error("Failed to create logfile.", e);
                    }
                }
            }
            final LogConfigurator config = new LogConfigurator();
            config.setRootLevel(logLevel);
            config.setFileName(logFile.getAbsolutePath());
            config.setFilePattern("%d %p %c{1} - %m%n");
            config.setMaxFileSize(5242880);
            config.setMaxBackupSize(0);
            config.configure();
            final Logger root = Logger.getRootLogger();
            for (final Map.Entry<String, StatusAppender> appender : appenderList.entrySet()) {
                root.addAppender(appender.getValue());
            }
            initialized = true;
        }
    }

    public static synchronized boolean deleteLog() {
        if (logFile == null) {
            return false;
        }
        boolean success = false;
        if (logFile.delete()) {
            success = true;
            initialized = false;
            try {
                configureLog();
                log.info("Log file recreated.");
            } catch (Exception e) {
                log.warn("Error while configuring file logging.");
            }
        } else {
            log.warn("Couldn't delete log file.");
        }
        return success;
    }

    public static synchronized String addLogListener(final LogListener listener) {
        try {
            configureLog();
        } catch (Exception e) {
            log.warn("Error while configuring file logging.");
        }
        StatusAppender appender = new StatusAppender(listener);
        String id = UUID.randomUUID().toString();
        appenderList.put(id, appender);
        Logger.getRootLogger().addAppender(appender);
        return id;
    }

    public static synchronized void removeLogListener(final String listenerId) {
        StatusAppender appender = appenderList.remove(listenerId);
        appender.close();
        Logger.getRootLogger().removeAppender(appender);
    }

    public static synchronized void destroy() {
        LogManager.shutdown();
        Enumeration e = Logger.getRootLogger().getAllAppenders();
        while (e.hasMoreElements()) {
            ((StatusAppender) e.nextElement()).close();
        }
        appenderList.clear();
        Logger.getRootLogger().removeAllAppenders();
        initialized = false;
    }

    public static void setLogLevel(@NonNull final Level logLevel) {
        Logging.logLevel = logLevel;
        Logger.getRootLogger().setLevel(logLevel);
    }

    public static String getLogFilePath() {
        if (logFile == null) {
            return null;
        }
        return logFile.getAbsolutePath();
    }

}
