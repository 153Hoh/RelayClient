package relay.petko.relay.utils;

import java.util.List;

public interface DataFromServerCallback {
    void onDataReceived(String from, List<String> data);
}
