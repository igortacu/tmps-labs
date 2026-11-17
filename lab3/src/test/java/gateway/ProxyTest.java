package gateway;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.domain.InferenceService;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.proxy.AccessDeniedException;
import gateway.patterns.proxy.AllowlistPolicy;
import gateway.patterns.proxy.AuthorizationProxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProxyTest {
    private final InferenceRequest request = new InferenceRequest("hello", "alice");

    @Test void unauthorizedCallNeverReachesRealService() {
        SimulatedBackend leaf = new SimulatedBackend("a", "[A]");
        InferenceService proxy = new AuthorizationProxy(leaf, new AllowlistPolicy(Set.of("bob")));
        assertThrows(AccessDeniedException.class, () -> proxy.complete(request));
        assertEquals(0, leaf.calls());
    }

    @Test void authorizedCallReturnsRealResponseUnchanged() {
        InferenceResponse expected = new InferenceResponse("exact response", "real");
        AtomicInteger calls = new AtomicInteger();
        InferenceService real = ignored -> { calls.incrementAndGet(); return expected; };
        InferenceService proxy = new AuthorizationProxy(real, new AllowlistPolicy(Set.of("alice")));
        assertSame(expected, proxy.complete(request));
        assertEquals(1, calls.get());
    }

    @Test void newPolicyWorksWithoutChangingProxy() {
        InferenceService proxy = new AuthorizationProxy(new SimulatedBackend("a", "[A]"),
            clientId -> clientId.startsWith("a"));
        assertEquals("a", proxy.complete(request).provider());
        assertThrows(AccessDeniedException.class,
            () -> proxy.complete(new InferenceRequest("hello", "bob")));
    }
}
