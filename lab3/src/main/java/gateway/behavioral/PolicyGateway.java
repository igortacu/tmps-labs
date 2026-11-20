package gateway.behavioral;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;
import gateway.patterns.proxy.AccessDeniedException;
import java.util.Objects;

/** Applies an Interpreter expression before delegating to a routing service. */
public final class PolicyGateway implements InferenceService {
    private final Interpreter.Expression rule;
    private final InferenceService service;

    public PolicyGateway(Interpreter.Expression rule, InferenceService service) {
        this.rule = Objects.requireNonNull(rule);
        this.service = Objects.requireNonNull(service);
    }

    @Override public InferenceResponse complete(InferenceRequest request) {
        if (!rule.evaluate(request)) throw new AccessDeniedException(request.clientId());
        return service.complete(request);
    }
}
