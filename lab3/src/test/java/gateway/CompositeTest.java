package gateway;

import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.patterns.composite.BackendGroup;
import gateway.patterns.composite.BackendNode;
import gateway.patterns.composite.SimulatedBackend;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompositeTest {
    private final InferenceRequest request = new InferenceRequest("hello", "alice");

    @Test void leafAndGroupShareCompleteContract() {
        SimulatedBackend leaf = new SimulatedBackend("a", "[A]");
        BackendNode group = new BackendGroup("cluster").add(leaf);
        assertEquals(leaf.complete(request), group.complete(request));
        assertEquals(2, leaf.calls());
    }

    @Test void nestedGroupsFallBackToAnotherChild() {
        SimulatedBackend failing = new SimulatedBackend("a", "[A]", 1);
        SimulatedBackend backup = new SimulatedBackend("b", "[B]");
        BackendNode region = new BackendGroup("region").add(failing);
        BackendNode root = new BackendGroup("root").add(region).add(backup);
        assertEquals("b", root.complete(request).provider());
        assertEquals(1, failing.calls());
        assertEquals(1, backup.calls());
    }

    @Test void removedChildLeavesGroupUnavailable() {
        SimulatedBackend leaf = new SimulatedBackend("a", "[A]");
        BackendNode group = new BackendGroup("empty").add(leaf).remove(leaf);
        assertThrows(BackendUnavailableException.class, () -> group.complete(request));
    }
}
