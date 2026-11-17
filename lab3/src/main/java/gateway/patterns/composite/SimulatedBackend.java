package gateway.patterns.composite;

import gateway.domain.BackendUnavailableException;
import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;

/** Leaf: one simulated LLM provider. */
public final class SimulatedBackend implements BackendNode {
    private final String name;
    private final String prefix;
    private int failuresRemaining;
    private int calls;

    public SimulatedBackend(String name, String prefix, int failuresBeforeSuccess) {
        if (failuresBeforeSuccess < 0) {
            throw new IllegalArgumentException("failuresBeforeSuccess must be nonnegative");
        }
        this.name = name;
        this.prefix = prefix;
        this.failuresRemaining = failuresBeforeSuccess;
    }

    public SimulatedBackend(String name, String prefix) {
        this(name, prefix, 0);
    }

    @Override public String name() { return name; }
    public int calls() { return calls; }

    @Override
    public InferenceResponse complete(InferenceRequest request) {
        calls++;
        if (failuresRemaining > 0) {
            failuresRemaining--;
            throw new BackendUnavailableException(name + " temporarily unavailable");
        }
        return new InferenceResponse(prefix + " " + request.prompt(), name);
    }
}
