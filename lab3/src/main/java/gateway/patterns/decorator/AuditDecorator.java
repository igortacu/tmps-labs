package gateway.patterns.decorator;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;

/** Concrete decorator: record success or failure without changing the response. */
public final class AuditDecorator extends ServiceDecorator {
    private final AuditSink sink;

    public AuditDecorator(InferenceService inner, AuditSink sink) {
        super(inner);
        this.sink = sink;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        InferenceResponse response;
        try {
            response = inner.complete(request);
        } catch (RuntimeException error) {
            sink.record(new AuditEvent(request.clientId(), "failure", null));
            throw error;
        }
        sink.record(new AuditEvent(request.clientId(), "success", response.provider()));
        return response;
    }
}
