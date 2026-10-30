# AI Inference Gateway

A small Python gateway that distributes inference requests across simulated LLM backends. Lab 1 focuses on three SOLID principles: Single Responsibility, Open/Closed, and Dependency Inversion. The next step adds five creational patterns. Backend latency and failures are simulated; benchmark results compare balancing strategies rather than real LLM services.

## Lab 1: Core gateway

### SOLID principles in this implementation

| Principle | Application |
|---|---|
| **Single Responsibility (S)** | `Gateway` coordinates requests, balancing strategies select a backend, adapters translate vendor interfaces, and the factory builds the configured components. |
| **Open/Closed (O)** | Add a backend or balancing strategy as a new implementation and register it; the gateway does not need edits. See `test_new_strategy_needs_no_core_change` in `tests/test_lab1.py`. |
| **Dependency Inversion (D)** | `Gateway` receives `Backend` and `BalancingStrategy` abstractions. Concrete classes are wired in `factory.py`. |

### Patterns and concurrency

Lab 1 also uses Strategy (`RoundRobin`, `LeastConnections`, `RandomChoice`), Adapter (`VendorAAdapter`, `VendorBAdapter`), Factory (`build_gateway` and registries), and Facade (`Gateway.handle`). Locks protect the round-robin counter and gateway load map for concurrent requests.

The detailed design notes, class diagram, benchmark example, and SOLID violation/fix are in [`docs/lab1.md`](docs/lab1.md).

For the class relationships, request flow, pattern roles, and arrow meanings, see [`docs/architecture.md`](docs/architecture.md). Lab 2 is documented in [`docs/lab2.md`](docs/lab2.md), with a shorter pattern reference in [`docs/creational.md`](docs/creational.md).

## Roadmap

The core gateway and creational patterns are implemented. Further phases are planned:

1. **Core gateway and SOLID:** backend abstraction and adapters, balancing strategies, gateway orchestration, configuration factory, concurrency handling.
2. **Creational patterns (current):** singleton registry, fluent builder, configuration prototype, optional vendor client pool, and vendor abstract factories.
3. **Resilience:** circuit breaker (State pattern), retries and timeouts (Decorator pattern).
4. **Traffic controls:** rate limiting and caching (Chain of Responsibility).
5. **Observability:** metrics and event notifications (Observer pattern).

## Run

From the repository root:

```sh
python3 -m unittest discover -s tests -v
python3 bench.py
```
