package gateway.patterns.proxy;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;

/** Proxy: check access before delegating through the same interface. */
public final class AuthorizationProxy implements InferenceService {
    private final InferenceService realService;
    private final AccessPolicy policy;

    public AuthorizationProxy(InferenceService realService, AccessPolicy policy) {
        this.realService = realService;
        this.policy = policy;
    }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        if (!policy.allows(request.clientId())) {
            throw new AccessDeniedException(request.clientId());
        }
        return realService.complete(request);
    }
}
