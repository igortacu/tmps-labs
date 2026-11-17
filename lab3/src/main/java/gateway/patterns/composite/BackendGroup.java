package gateway.patterns.composite;

import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import java.util.ArrayList;
import java.util.List;

/** Composite: try leaves or nested groups through the same interface. */
public final class BackendGroup implements BackendNode {
    private final String name;
    private final List<BackendNode> children = new ArrayList<>();

    public BackendGroup(String name) { this.name = name; }
    @Override public String name() { return name; }

    public BackendGroup add(BackendNode child) {
        if (child == this) throw new IllegalArgumentException("a group cannot contain itself");
        children.add(child);
        return this;
    }

    public BackendGroup remove(BackendNode child) {
        children.remove(child);
        return this;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        for (BackendNode child : children) {
            try {
                return child.complete(request);
            } catch (BackendUnavailableException ignored) {
                // Try the next child; unexpected errors propagate.
            }
        }
        throw new BackendUnavailableException("all backends in " + name + " unavailable");
    }
}
