package gateway;

import gateway.creation.GatewayPipelineBuilder;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;
import gateway.patterns.composite.BackendGroup;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.decorator.AuditDecorator;
import gateway.patterns.decorator.AuditEvent;
import gateway.patterns.decorator.RetryDecorator;
import gateway.patterns.decorator.ServiceDecorator;
import gateway.patterns.proxy.AccessDeniedException;
import gateway.patterns.proxy.AllowlistPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private final InferenceRequest request = new InferenceRequest("hello", "alice");

    @Test void builderConnectsProxyDecoratorsAndCompositeInOnePath() {
        SimulatedBackend a = new SimulatedBackend("a", "[A]", 1);
        SimulatedBackend b = new SimulatedBackend("b", "[B]", 1);
        BackendGroup region = new BackendGroup("region").add(a).add(b);
        List<AuditEvent> events = new ArrayList<>();
        InferenceService gateway = new GatewayPipelineBuilder()
            .addBackend(region)
            .decorate(inner -> new RetryDecorator(inner, 2))
            .decorate(inner -> new AuditDecorator(inner, events::add))
            .withPolicy(new AllowlistPolicy(Set.of("alice")))
            .build();
        assertEquals("a", gateway.complete(request).provider());
        assertEquals(2, a.calls());
        assertEquals(1, b.calls());
        assertEquals(List.of("success"), events.stream().map(AuditEvent::outcome).toList());
        assertThrows(AccessDeniedException.class,
            () -> gateway.complete(new InferenceRequest("hello", "bob")));
        assertEquals(2, a.calls());
    }

    @Test void builderAcceptsNewDecoratorWithoutModification() {
        class UppercaseDecorator extends ServiceDecorator {
            UppercaseDecorator(InferenceService inner) { super(inner); }
            @Override public InferenceResponse complete(InferenceRequest request) {
                InferenceResponse response = inner.complete(request);
                return new InferenceResponse(response.text().toUpperCase(), response.provider());
            }
        }
        InferenceService gateway = new GatewayPipelineBuilder()
            .addBackend(new SimulatedBackend("a", "[A]"))
            .decorate(UppercaseDecorator::new)
            .withPolicy(clientId -> true)
            .build();
        assertEquals("[A] HELLO", gateway.complete(request).text());
    }
}
