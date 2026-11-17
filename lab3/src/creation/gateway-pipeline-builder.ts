import type { InferenceService } from "../domain/inference.js";
import type { BackendNode } from "../patterns/composite/backend-node.js";
import { BackendGroup } from "../patterns/composite/backend-group.js";
import { AuthorizationProxy, type AccessPolicy } from "../patterns/proxy/authorization-proxy.js";

/** Creational Builder reused to assemble the structural pattern chain. */
export class GatewayPipelineBuilder {
  private readonly backends: BackendNode[] = [];
  private readonly wrappers: Array<(inner: InferenceService) => InferenceService> = [];
  private policy?: AccessPolicy;

  addBackend(backend: BackendNode): this {
    this.backends.push(backend);
    return this;
  }

  decorate(wrapper: (inner: InferenceService) => InferenceService): this {
    this.wrappers.push(wrapper);
    return this;
  }

  withPolicy(policy: AccessPolicy): this {
    this.policy = policy;
    return this;
  }

  build(): InferenceService {
    if (this.backends.length === 0) throw new Error("at least one backend is required");
    if (!this.policy) throw new Error("an access policy is required");

    const group = new BackendGroup("gateway");
    for (const backend of this.backends) group.add(backend);
    let service: InferenceService = group;
    for (const wrapper of this.wrappers) service = wrapper(service);
    return new AuthorizationProxy(service, this.policy);
  }
}
