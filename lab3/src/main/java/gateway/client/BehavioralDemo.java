package gateway.client;

import gateway.behavioral.Interpreter;
import gateway.behavioral.PolicyGateway;
import gateway.behavioral.RoutePlan;
import gateway.behavioral.Routing;
import gateway.behavioral.RoutingEvents;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceService;
import gateway.patterns.composite.SimulatedBackend;
import gateway.patterns.proxy.AccessDeniedException;
import java.util.List;

/** One request path demonstrates Interpreter, Strategy, Observer, and Memento. */
public final class BehavioralDemo {
    public static void main(String[] args) {
        var east = new SimulatedBackend("east", "East:");
        var west = new SimulatedBackend("west", "West:");
        var routing = new Routing.Context(List.of(east, west), new Routing.Forward());
        var log = new RoutingEvents.ChangeLog();
        var count = new RoutingEvents.ChangeCount();
        routing.subscribe(log);
        routing.subscribe(count);

        Interpreter.Expression rule = new Interpreter.And(
            new Interpreter.ClientIs("student"),
            new Interpreter.PromptContains("patterns"));
        InferenceService gateway = new PolicyGateway(rule, routing);
        var allowed = new InferenceRequest("explain patterns", "student");
        var denied = new InferenceRequest("explain patterns", "guest");

        System.out.println("Interpreter + Strategy: " + gateway.complete(allowed));
        try { gateway.complete(denied); }
        catch (AccessDeniedException expected) { System.out.println("Interpreter: guest denied"); }

        Routing.Context.Snapshot snapshot = routing.save();
        routing.setStrategy(new Routing.Reverse());
        System.out.println("Strategy switched: " + gateway.complete(allowed));
        routing.restore(snapshot);
        System.out.println("Memento restored: " + gateway.complete(allowed));
        System.out.println("Observer log: " + log.entries() + "; changes=" + count.count());
        routing.unsubscribe(count);

        RoutePlan.Node plan = new RoutePlan.Group("global", List.of(
            new RoutePlan.Backend(east.name()), new RoutePlan.Backend(west.name())));
        System.out.println("Visitor description: " + plan.accept(new RoutePlan.Describe()));
        System.out.println("Visitor count: " + plan.accept(new RoutePlan.CountBackends()));
    }
}
