import { GatewayPipelineBuilder } from "../creation/gateway-pipeline-builder.js";
import { BackendGroup } from "../patterns/composite/backend-group.js";
import { SimulatedBackend } from "../patterns/composite/simulated-backend.js";
import { AuditDecorator, type AuditEvent } from "../patterns/decorator/audit-decorator.js";
import { RetryDecorator } from "../patterns/decorator/retry-decorator.js";
import { AccessDeniedError, AllowlistPolicy } from "../patterns/proxy/authorization-proxy.js";

const region = new BackendGroup("europe")
  .add(new SimulatedBackend("vendor-a", "[A]", 1))
  .add(new SimulatedBackend("vendor-b", "[B]", 1));
const events: AuditEvent[] = [];
const gateway = new GatewayPipelineBuilder()
  .addBackend(region)
  .decorate((inner) => new RetryDecorator(inner, 2))
  .decorate((inner) => new AuditDecorator(inner, { record: (event) => events.push(event) }))
  .withPolicy(new AllowlistPolicy(new Set(["demo-client"])))
  .build();

console.log("Path: AuthorizationProxy -> AuditDecorator -> RetryDecorator -> BackendGroup -> SimulatedBackend");
const response = gateway.complete({ clientId: "demo-client", prompt: "hello" });
console.log(`Allowed: ${response.provider} replied ${response.text}`);
console.log(`Audit: ${JSON.stringify(events)}`);
try {
  gateway.complete({ clientId: "unknown", prompt: "hello" });
} catch (error) {
  if (!(error instanceof AccessDeniedError)) throw error;
  console.log(`Blocked: ${error.message}`);
}
