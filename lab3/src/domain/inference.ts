export interface InferenceRequest {
  readonly prompt: string;
  readonly clientId: string;
}

export interface InferenceResponse {
  readonly text: string;
  readonly provider: string;
}

/** Shared contract for the composite, decorators, and proxy. */
export interface InferenceService {
  complete(request: InferenceRequest): InferenceResponse;
}

export class BackendUnavailableError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "BackendUnavailableError";
  }
}
