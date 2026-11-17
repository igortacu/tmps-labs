package gateway.client;

import gateway.creation.GatewayPipelineBuilder;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;
import gateway.patterns.composite.BackendGroup;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.decorator.AuditDecorator;
import gateway.patterns.decorator.AuditEvent;
import gateway.patterns.decorator.RetryDecorator;
import gateway.patterns.proxy.AccessDeniedException;
import gateway.patterns.proxy.AllowlistPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Demo {
    private Demo() {}

    public static void main(String[] args) {
        BackendGroup region = new BackendGroup("europe")
            .add(new SimulatedBackend("vendor-a", "[A]", 1))
            .add(new SimulatedBackend("vendor-b", "[B]", 1));
        List<AuditEvent> events = new ArrayList<>();
        InferenceService gateway = new GatewayPipelineBuilder()
            .addBackend(region)
            .decorate(inner -> new RetryDecorator(inner, 2))
            .decorate(inner -> new AuditDecorator(inner, events::add))
            .withPolicy(new AllowlistPolicy(Set.of("demo-client")))
            .build();

        System.out.println("Path: AuthorizationProxy -> AuditDecorator -> RetryDecorator -> BackendGroup -> SimulatedBackend");
        InferenceResponse response = gateway.complete(new InferenceRequest("hello", "demo-client"));
        System.out.println("Allowed: " + response.provider() + " replied " + response.text());
        System.out.println("Audit: " + events);
        try {
            gateway.complete(new InferenceRequest("hello", "unknown"));
        } catch (AccessDeniedException error) {
            System.out.println("Blocked: " + error.getMessage());
        }
    }
}
