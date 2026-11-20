package gateway.behavioral;

import gateway.domain.*;
import gateway.patterns.composite.BackendNode;
import java.util.*;

/** Strategy, Memento, and Observer meet in a configurable routing context. */
public final class Routing {
    private Routing() {}
    public interface Strategy { List<BackendNode> order(List<BackendNode> backends); }
    public static final class Forward implements Strategy {
        @Override public List<BackendNode> order(List<BackendNode> backends) { return List.copyOf(backends); }
    }
    public static final class Reverse implements Strategy {
        @Override public List<BackendNode> order(List<BackendNode> backends) {
            var copy = new ArrayList<>(backends); Collections.reverse(copy); return copy;
        }
    }
    public static final class Context implements InferenceService {
        private final List<BackendNode> backends;
        private final List<RoutingEvents.Observer> observers = new ArrayList<>();
        private Strategy strategy;
        public Context(List<BackendNode> backends, Strategy strategy) {
            if (backends.isEmpty()) throw new IllegalArgumentException("At least one backend required");
            this.backends = List.copyOf(backends);
            this.strategy = Objects.requireNonNull(strategy);
        }
        public Strategy strategy() { return strategy; }
        public void subscribe(RoutingEvents.Observer observer) { observers.add(Objects.requireNonNull(observer)); }
        public void unsubscribe(RoutingEvents.Observer observer) { observers.remove(observer); }
        public void setStrategy(Strategy strategy) {
            Strategy next = Objects.requireNonNull(strategy);
            if (this.strategy == next) return;
            String previous = this.strategy.getClass().getSimpleName();
            this.strategy = next;
            for (RoutingEvents.Observer observer : List.copyOf(observers)) {
                observer.strategyChanged(previous, next.getClass().getSimpleName());
            }
        }
        public Snapshot save() { return new Snapshot(strategy); }
        public void restore(Snapshot snapshot) { setStrategy(snapshot.strategy); }
        public static final class Snapshot {
            private final Strategy strategy;
            private Snapshot(Strategy strategy) { this.strategy = strategy; }
        }
        @Override public InferenceResponse complete(InferenceRequest request) {
            BackendUnavailableException last = null;
            for (BackendNode backend : strategy.order(backends)) {
                try { return backend.complete(request); }
                catch (BackendUnavailableException ex) { last = ex; }
            }
            throw new BackendUnavailableException("All routes failed: " + last);
        }
    }
}
