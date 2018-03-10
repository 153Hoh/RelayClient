package relay.petko.relay.log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

class StatusAppender extends AppenderSkeleton {

    private final LogListener listener;

    StatusAppender(final LogListener listener) {
        this.listener = listener;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(final LoggingEvent event) {
        listener.onLogMessage(event.getMessage().toString());
    }

}
