import {
  BackendUnavailableError,
  type InferenceRequest,
  type InferenceResponse,
  type InferenceService,
} from "../../domain/inference.js";
import { ServiceDecorator } from "./service-decorator.js";

/** Concrete decorator: retry temporary backend unavailability. */
export class RetryDecorator extends ServiceDecorator {
  constructor(inner: InferenceService, private readonly maxAttempts: number) {
    super(inner);
    if (!Number.isInteger(maxAttempts) || maxAttempts < 1) {
      throw new RangeError("maxAttempts must be a positive integer");
    }
  }

  override complete(request: InferenceRequest): InferenceResponse {
    for (let attempt = 1; attempt <= this.maxAttempts; attempt++) {
      try {
        return this.inner.complete(request);
      } catch (error) {
        if (!(error instanceof BackendUnavailableError) || attempt === this.maxAttempts) {
          throw error;
        }
      }
    }
    throw new Error("unreachable retry state");
  }
}
