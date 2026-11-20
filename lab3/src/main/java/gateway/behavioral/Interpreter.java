package gateway.behavioral;

import gateway.domain.InferenceRequest;
import java.util.Objects;

/** Interpreter: a small request-rule grammar assembled from expression objects. */
public final class Interpreter {
    private Interpreter() {}
    public interface Expression { boolean evaluate(InferenceRequest request); }
    public record ClientIs(String id) implements Expression {
        @Override public boolean evaluate(InferenceRequest request) { return id.equals(request.clientId()); }
    }
    public record PromptContains(String text) implements Expression {
        @Override public boolean evaluate(InferenceRequest request) { return request.prompt().contains(text); }
    }
    public record And(Expression left, Expression right) implements Expression {
        public And { Objects.requireNonNull(left); Objects.requireNonNull(right); }
        @Override public boolean evaluate(InferenceRequest request) {
            return left.evaluate(request) && right.evaluate(request);
        }
    }
    public record Or(Expression left, Expression right) implements Expression {
        public Or { Objects.requireNonNull(left); Objects.requireNonNull(right); }
        @Override public boolean evaluate(InferenceRequest request) {
            return left.evaluate(request) || right.evaluate(request);
        }
    }
}
