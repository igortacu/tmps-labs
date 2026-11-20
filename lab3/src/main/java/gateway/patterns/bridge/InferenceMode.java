package gateway.patterns.bridge;

import gateway.patterns.composite.BackendNode;
import java.util.Objects;

/** Bridge abstraction: request modes vary independently of backend implementations. */
public abstract class InferenceMode implements BackendNode {
    protected final BackendNode backend;

    protected InferenceMode(BackendNode backend) {
        this.backend = Objects.requireNonNull(backend);
    }
}
