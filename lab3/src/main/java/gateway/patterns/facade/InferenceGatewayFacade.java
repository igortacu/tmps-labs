package gateway.patterns.facade;

import gateway.creation.GatewayPipelineBuilder;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;
import gateway.patterns.composite.BackendNode;
import gateway.patterns.decorator.AuditDecorator;
import gateway.patterns.decorator.AuditSink;
import gateway.patterns.decorator.RetryDecorator;
import gateway.patterns.proxy.AccessPolicy;
import java.util.Objects;

/** Facade: assemble the gateway subsystem and expose one ask operation. */
public final class InferenceGatewayFacade {
    private final InferenceService pipeline;

    public InferenceGatewayFacade(BackendNode backend, AccessPolicy policy,
                                  AuditSink audit, int maxAttempts) {
        Objects.requireNonNull(backend);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(audit);
        pipeline = new GatewayPipelineBuilder()
            .addBackend(backend)
            .decorate(inner -> new RetryDecorator(inner, maxAttempts))
            .decorate(inner -> new AuditDecorator(inner, audit))
            .withPolicy(policy)
            .build();
    }

    public InferenceResponse ask(String prompt, String clientId) {
        return pipeline.complete(new InferenceRequest(prompt, clientId));
    }
}
