package gateway;

import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceService;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.decorator.AuditDecorator;
import gateway.patterns.decorator.AuditEvent;
import gateway.patterns.decorator.RetryDecorator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecoratorTest {
    private final InferenceRequest request = new InferenceRequest("hello", "alice");

    @Test void retrySucceedsAfterTemporaryFailure() {
        SimulatedBackend leaf = new SimulatedBackend("a", "[A]", 1);
        InferenceService service = new RetryDecorator(leaf, 2);
        assertEquals("a", service.complete(request).provider());
        assertEquals(2, leaf.calls());
    }

    @Test void auditRecordsFailureAndSuccessWithoutChangingResponse() {
        List<AuditEvent> events = new ArrayList<>();
        InferenceService service = new AuditDecorator(new SimulatedBackend("a", "[A]", 1), events::add);
        assertThrows(BackendUnavailableException.class, () -> service.complete(request));
        assertEquals("[A] hello", service.complete(request).text());
        assertEquals(List.of("failure", "success"), events.stream().map(AuditEvent::outcome).toList());
    }

    @Test void decoratorsCanBeStackedInEitherOrder() {
        for (boolean auditOutside : new boolean[] {true, false}) {
            List<AuditEvent> events = new ArrayList<>();
            SimulatedBackend leaf = new SimulatedBackend("a", "[A]", 1);
            InferenceService service = auditOutside
                ? new AuditDecorator(new RetryDecorator(leaf, 2), events::add)
                : new RetryDecorator(new AuditDecorator(leaf, events::add), 2);
            assertEquals("a", service.complete(request).provider());
            assertEquals("success", events.get(events.size() - 1).outcome());
            assertEquals(2, leaf.calls());
        }
    }
}
