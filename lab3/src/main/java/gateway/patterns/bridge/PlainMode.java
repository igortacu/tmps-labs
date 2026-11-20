package gateway.patterns.bridge;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.patterns.composite.BackendNode;

/** Refined abstraction: send the request unchanged. */
public final class PlainMode extends InferenceMode {
    public PlainMode(BackendNode backend) { super(backend); }
    @Override public String name() { return backend.name(); }
    @Override public InferenceResponse complete(InferenceRequest request) {
        return backend.complete(request);
    }
}
