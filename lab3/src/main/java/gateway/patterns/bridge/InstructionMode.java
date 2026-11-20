package gateway.patterns.bridge;

import gateway.domain.InferenceRequest;
import gateway.domain.InferenceResponse;
import gateway.patterns.composite.BackendNode;
import java.util.Objects;

/** Refined abstraction: attach a reusable instruction before delegating. */
public final class InstructionMode extends InferenceMode {
    private final String instruction;

    public InstructionMode(BackendNode backend, String instruction) {
        super(backend);
        this.instruction = Objects.requireNonNull(instruction);
    }

    @Override public String name() { return backend.name(); }
    @Override public InferenceResponse complete(InferenceRequest request) {
        InferenceRequest prepared = new InferenceRequest(
            instruction + "\n" + request.prompt(), request.clientId());
        return backend.complete(prepared);
    }
}
