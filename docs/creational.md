# Creational patterns in the gateway

These patterns extend the original gateway without changing `Gateway.handle` or its backend and strategy contracts.

| Pattern | Implementation | Role |
|---|---|---|
| Singleton | `FactoryRegistry` | One process-wide, lock-protected registry for backend and strategy constructors. `register_backend` and `register_strategy` use it. |
| Builder | `GatewayBuilder` | Fluent configuration that ends in the existing `build_gateway` function. |
| Prototype | `GatewayConfigPrototype` | Deep copies a reusable configuration template, so edits to a clone do not affect later clones. |
| Object Pool | `ObjectPool` | Bounded, thread-safe leases for reusable vendor clients. `pool_size` enables it per backend. |
| Abstract Factory | `BackendFamilyFactory`, `VendorAFactory`, `VendorBFactory` | Each vendor factory creates its matching client and adapter. The registry uses these factories for built-in backend types. |

## Example

```python
from gateway import GatewayBuilder, GatewayConfigPrototype, Request

base = GatewayConfigPrototype({
    "strategy": "round_robin",
    "backends": [{"type": "vendor_a", "name": "fast-a", "latency_s": 0.01}],
})

gateway = (GatewayBuilder(base)
           .add_backend("vendor_b", name="pooled-b", latency_s=0.03, pool_size=4)
           .build())
print(gateway.handle(Request("hello")).text)

# A separate clone can be customized without changing the template.
config = base.clone(strategy="least_connections")
```

`pool_size` is optional. When omitted, an adapter keeps one client as before. A positive value creates at most that many clients for the backend. A request waits when every client is leased, and the client returns to the pool even if the vendor call fails. Pooling is useful when a client has expensive setup or cannot be used concurrently; these simulated clients are cheap, so the setting mainly demonstrates the pattern.

The registry is process-wide, but each call to `build_gateway` still creates a separate gateway and strategy. Custom backends and strategies continue to work through `register_backend` and `register_strategy`.
