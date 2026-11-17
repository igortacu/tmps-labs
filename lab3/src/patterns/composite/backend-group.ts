import {
  BackendUnavailableError,
  type InferenceRequest,
  type InferenceResponse,
} from "../../domain/inference.js";
import type { BackendNode } from "./backend-node.js";

/** Composite: try children in order; children may be leaves or other groups. */
export class BackendGroup implements BackendNode {
  private readonly children: BackendNode[] = [];

  constructor(public readonly name: string) {}

  add(child: BackendNode): this {
    if (child === this) throw new Error("a group cannot contain itself");
    this.children.push(child);
    return this;
  }

  remove(child: BackendNode): this {
    const index = this.children.indexOf(child);
    if (index >= 0) this.children.splice(index, 1);
    return this;
  }

  complete(request: InferenceRequest): InferenceResponse {
    for (const child of this.children) {
      try {
        return child.complete(request);
      } catch (error) {
        if (!(error instanceof BackendUnavailableError)) throw error;
      }
    }
    throw new BackendUnavailableError(`all backends in ${this.name} unavailable`);
  }
}
