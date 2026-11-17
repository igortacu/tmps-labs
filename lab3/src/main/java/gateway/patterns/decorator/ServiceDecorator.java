package gateway.patterns.decorator;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;

/** Base decorator: every concrete wrapper retains the service interface. */
public abstract class ServiceDecorator implements InferenceService {
    protected final InferenceService inner;

    protected ServiceDecorator(InferenceService inner) { this.inner = inner; }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        return inner.complete(request);
    }
}
