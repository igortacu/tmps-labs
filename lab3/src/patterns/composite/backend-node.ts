import type { InferenceService } from "../../domain/inference.js";

/** Component: a leaf and a group are interchangeable to callers. */
export interface BackendNode extends InferenceService {
  readonly name: string;
}
