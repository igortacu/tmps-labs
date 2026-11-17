import type {
  InferenceRequest,
  InferenceResponse,
  InferenceService,
} from "../../domain/inference.js";
import { ServiceDecorator } from "./service-decorator.js";

export interface AuditEvent {
  readonly clientId: string;
  readonly outcome: "success" | "failure";
  readonly provider?: string;
}

export interface AuditSink {
  record(event: AuditEvent): void;
}

/** Concrete decorator: record the outcome of a delegated call. */
export class AuditDecorator extends ServiceDecorator {
  constructor(inner: InferenceService, private readonly sink: AuditSink) {
    super(inner);
  }

  override complete(request: InferenceRequest): InferenceResponse {
    let response: InferenceResponse;
    try {
      response = this.inner.complete(request);
    } catch (error) {
      this.sink.record({ clientId: request.clientId, outcome: "failure" });
      throw error;
    }
    this.sink.record({ clientId: request.clientId, outcome: "success", provider: response.provider });
    return response;
  }
}
