package gateway.domain;

/** Shared contract for the composite, decorators, and proxy. */
public interface InferenceService {
    InferenceResponse complete(InferenceRequest request);
}
