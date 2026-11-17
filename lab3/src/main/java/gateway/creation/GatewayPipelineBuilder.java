package gateway.creation;

import gateway.domain.InferenceService;
import gateway.patterns.composite.BackendGroup;
import gateway.patterns.composite.BackendNode;
import gateway.patterns.proxy.AccessPolicy;
import gateway.patterns.proxy.AuthorizationProxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Reuses Builder to assemble the three structural patterns. */
public final class GatewayPipelineBuilder {
    private final List<BackendNode> backends = new ArrayList<>();
    private final List<UnaryOperator<InferenceService>> wrappers = new ArrayList<>();
    private AccessPolicy policy;

    public GatewayPipelineBuilder addBackend(BackendNode backend) {
        backends.add(backend);
        return this;
    }

    public GatewayPipelineBuilder decorate(UnaryOperator<InferenceService> wrapper) {
        wrappers.add(wrapper);
        return this;
    }

    public GatewayPipelineBuilder withPolicy(AccessPolicy policy) {
        this.policy = policy;
        return this;
    }

    public InferenceService build() {
        if (backends.isEmpty()) throw new IllegalStateException("at least one backend is required");
        if (policy == null) throw new IllegalStateException("an access policy is required");
        BackendGroup group = new BackendGroup("gateway");
        for (BackendNode backend : backends) group.add(backend);
        InferenceService service = group;
        for (UnaryOperator<InferenceService> wrapper : wrappers) service = wrapper.apply(service);
        return new AuthorizationProxy(service, policy);
    }
}
