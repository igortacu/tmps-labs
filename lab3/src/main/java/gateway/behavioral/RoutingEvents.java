package gateway.behavioral;

import java.util.ArrayList;
import java.util.List;

/** Observer roles for routing policy changes. */
public final class RoutingEvents {
    private RoutingEvents() {}

    public interface Observer {
        void strategyChanged(String previous, String current);
    }

    public static final class ChangeLog implements Observer {
        private final List<String> entries = new ArrayList<>();

        @Override public void strategyChanged(String previous, String current) {
            entries.add(previous + " -> " + current);
        }

        public List<String> entries() { return List.copyOf(entries); }
    }

    public static final class ChangeCount implements Observer {
        private int count;

        @Override public void strategyChanged(String previous, String current) { count++; }

        public int count() { return count; }
    }
}
