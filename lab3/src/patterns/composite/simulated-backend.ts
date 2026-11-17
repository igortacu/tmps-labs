import {
  BackendUnavailableError,
  type InferenceRequest,
  type InferenceResponse,
} from "../../domain/inference.js";
import type { BackendNode } from "./backend-node.js";

/** Leaf: one simulated LLM provider. */
export class SimulatedBackend implements BackendNode {
  private failuresRemaining: number;
  private callCount = 0;

  constructor(
    public readonly name: string,
    private readonly prefix: string,
    failuresBeforeSuccess = 0,
  ) {
    if (failuresBeforeSuccess < 0 || !Number.isInteger(failuresBeforeSuccess)) {
      throw new RangeError("failuresBeforeSuccess must be a nonnegative integer");
    }
    this.failuresRemaining = failuresBeforeSuccess;
  }

  get calls(): number {
    return this.callCount;
  }

  complete(request: InferenceRequest): InferenceResponse {
    this.callCount++;
    if (this.failuresRemaining > 0) {
      this.failuresRemaining--;
      throw new BackendUnavailableError(`${this.name} temporarily unavailable`);
    }
    return { text: `${this.prefix} ${request.prompt}`, provider: this.name };
  }
}
