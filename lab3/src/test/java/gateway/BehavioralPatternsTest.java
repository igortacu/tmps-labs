package gateway;

import gateway.behavioral.*;
import gateway.domain.*;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.proxy.AccessDeniedException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BehavioralPatternsTest {
    private static final InferenceRequest REQUEST = new InferenceRequest("explain patterns", "student");
    private static SimulatedBackend backend(String name) { return new SimulatedBackend(name, name + ":"); }
    private static Routing.Context routing() {
        return new Routing.Context(List.of(backend("east"), backend("west")), new Routing.Forward());
    }

    @Test void interpreterCombinesTerminalExpressions() {
        Interpreter.Expression rule = new Interpreter.And(
            new Interpreter.ClientIs("student"), new Interpreter.PromptContains("patterns"));
        assertTrue(rule.evaluate(REQUEST));
        assertFalse(rule.evaluate(new InferenceRequest("explain patterns", "guest")));
    }

    @Test void interpreterOrAllowsEitherBranch() {
        Interpreter.Expression rule = new Interpreter.Or(
            new Interpreter.ClientIs("admin"), new Interpreter.PromptContains("patterns"));
        assertTrue(rule.evaluate(REQUEST));
        assertFalse(rule.evaluate(new InferenceRequest("hello", "guest")));
    }

    @Test void interpreterPolicyBlocksBackendCall() {
        var leaf = backend("east");
        InferenceService gateway = new PolicyGateway(new Interpreter.ClientIs("student"),
            new Routing.Context(List.of(leaf), new Routing.Forward()));
        assertThrows(AccessDeniedException.class,
            () -> gateway.complete(new InferenceRequest("hello", "guest")));
        assertEquals(0, leaf.calls());
    }

    @Test void mementoRestoresTheSavedRoutingChoice() {
        var context = routing();
        var snapshot = context.save();
        context.setStrategy(new Routing.Reverse());
        assertEquals("west", context.complete(REQUEST).provider());
        context.restore(snapshot);
        assertEquals("east", context.complete(REQUEST).provider());
    }

    @Test void mementoSurvivesLaterContextChanges() {
        var context = routing();
        var snapshot = context.save();
        context.setStrategy(new Routing.Reverse());
        context.setStrategy(new Routing.Forward());
        context.setStrategy(new Routing.Reverse());
        context.restore(snapshot);
        assertInstanceOf(Routing.Forward.class, context.strategy());
    }

    @Test void observerNotifiesTwoSubscribers() {
        var context = routing();
        var log = new RoutingEvents.ChangeLog();
        var count = new RoutingEvents.ChangeCount();
        context.subscribe(log);
        context.subscribe(count);
        context.setStrategy(new Routing.Reverse());
        assertEquals(List.of("Forward -> Reverse"), log.entries());
        assertEquals(1, count.count());
    }

    @Test void observerCanUnsubscribeAtRuntime() {
        var context = routing();
        var count = new RoutingEvents.ChangeCount();
        context.subscribe(count);
        context.setStrategy(new Routing.Reverse());
        context.unsubscribe(count);
        context.setStrategy(new Routing.Forward());
        assertEquals(1, count.count());
    }

    @Test void strategyCanBeSwappedAtRuntime() {
        var context = routing();
        assertEquals("east", context.complete(REQUEST).provider());
        context.setStrategy(new Routing.Reverse());
        assertEquals("west", context.complete(REQUEST).provider());
    }

    @Test void strategyFallsBackInItsChosenOrder() {
        var failing = new SimulatedBackend("east", "East:", 1);
        var context = new Routing.Context(List.of(failing, backend("west")), new Routing.Forward());
        assertEquals("west", context.complete(REQUEST).provider());
        assertEquals(1, failing.calls());
    }

    @Test void visitorCountsNestedBackends() {
        RoutePlan.Node plan = new RoutePlan.Group("global", List.of(
            new RoutePlan.Backend("east"),
            new RoutePlan.Group("backup", List.of(new RoutePlan.Backend("west")))));
        assertEquals(2, plan.accept(new RoutePlan.CountBackends()));
    }

    @Test void visitorAddsDescriptionWithoutChangingNodes() {
        RoutePlan.Node plan = new RoutePlan.Group("global", List.of(new RoutePlan.Backend("east")));
        assertEquals("global[east]", plan.accept(new RoutePlan.Describe()));
    }

    @Test void integratedPolicyRoutingSnapshotAndEvents() {
        var context = routing();
        var log = new RoutingEvents.ChangeLog();
        context.subscribe(log);
        InferenceService gateway = new PolicyGateway(new Interpreter.ClientIs("student"), context);
        var snapshot = context.save();
        assertEquals("east", gateway.complete(REQUEST).provider());
        context.setStrategy(new Routing.Reverse());
        assertEquals("west", gateway.complete(REQUEST).provider());
        context.restore(snapshot);
        assertEquals("east", gateway.complete(REQUEST).provider());
        assertEquals(List.of("Forward -> Reverse", "Reverse -> Forward"), log.entries());
    }
}
