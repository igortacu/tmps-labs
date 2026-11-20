package gateway.behavioral;

import java.util.*;

/** Visitor: stable plan nodes accept new reporting operations by double dispatch. */
public final class RoutePlan {
    private RoutePlan() {}
    public interface Node { <T> T accept(Visitor<T> visitor); }
    public interface Visitor<T> { T visitBackend(Backend backend); T visitGroup(Group group); }
    public record Backend(String name) implements Node {
        @Override public <T> T accept(Visitor<T> visitor) { return visitor.visitBackend(this); }
    }
    public record Group(String name, List<Node> children) implements Node {
        public Group { children = List.copyOf(children); }
        @Override public <T> T accept(Visitor<T> visitor) { return visitor.visitGroup(this); }
    }
    public static final class CountBackends implements Visitor<Integer> {
        @Override public Integer visitBackend(Backend backend) { return 1; }
        @Override public Integer visitGroup(Group group) {
            int count = 0;
            for (Node child : group.children()) count += child.accept(this);
            return count;
        }
    }
    public static final class Describe implements Visitor<String> {
        @Override public String visitBackend(Backend backend) { return backend.name(); }
        @Override public String visitGroup(Group group) {
            List<String> names = new ArrayList<>();
            for (Node child : group.children()) names.add(child.accept(this));
            return group.name() + "[" + String.join(", ", names) + "]";
        }
    }
}
