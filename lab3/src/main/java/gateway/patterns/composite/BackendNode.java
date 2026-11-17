package gateway.patterns.composite;

import gateway.domain.InferenceService;

/** Component: a leaf and a group are interchangeable. */
public interface BackendNode extends InferenceService {
    String name();
}
