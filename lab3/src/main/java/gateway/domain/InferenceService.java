package gateway.domain;

/** Shared contract for the inference gateway and its structural layers. */
public interface InferenceService {
    InferenceResponse complete(InferenceRequest request);
}
