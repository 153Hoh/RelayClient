package relay.petko.relay.log;

import android.support.annotation.Nullable;

import org.apache.log4j.lf5.LogLevel;

public interface ServerLogListener {
    void onServerLog(LogLevel level, Object message, @Nullable Throwable t);
}
