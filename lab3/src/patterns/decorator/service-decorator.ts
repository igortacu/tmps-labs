import type {
  InferenceRequest,
  InferenceResponse,
  InferenceService,
} from "../../domain/inference.js";

/** Base decorator: concrete decorators wrap the shared service interface. */
export abstract class ServiceDecorator implements InferenceService {
  protected constructor(protected readonly inner: InferenceService) {}

  complete(request: InferenceRequest): InferenceResponse {
    return this.inner.complete(request);
  }
}
