import type { InferenceRequest, InferenceResponse, InferenceService } from "../../domain/inference.js";

export interface AccessPolicy {
  allows(clientId: string): boolean;
}

export class AllowlistPolicy implements AccessPolicy {
  constructor(private readonly clients: ReadonlySet<string>) {}
  allows(clientId: string): boolean {
    return this.clients.has(clientId);
  }
}

export class AccessDeniedError extends Error {
  constructor(clientId: string) {
    super(`client ${clientId} is not authorized`);
    this.name = "AccessDeniedError";
  }
}

/** Proxy: enforce access while retaining the service's exact interface. */
export class AuthorizationProxy implements InferenceService {
  constructor(
    private readonly realService: InferenceService,
    private readonly policy: AccessPolicy,
  ) {}

  complete(request: InferenceRequest): InferenceResponse {
    if (!this.policy.allows(request.clientId)) {
      throw new AccessDeniedError(request.clientId);
    }
    return this.realService.complete(request);
  }
}
