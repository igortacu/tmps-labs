package gateway.patterns.decorator;

import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;

/** Concrete decorator: retry temporary backend unavailability. */
public final class RetryDecorator extends ServiceDecorator {
    private final int maxAttempts;

    public RetryDecorator(InferenceService inner, int maxAttempts) {
        super(inner);
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
        this.maxAttempts = maxAttempts;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return inner.complete(request);
            } catch (BackendUnavailableException error) {
                if (attempt >= maxAttempts) throw error;
            }
        }
    }
}
