# Architecture and design patterns

This document describes the Phase 1 gateway. The simulated vendor clients stand in for real vendor SDKs; they do not make network requests or generate real LLM responses.

## Class and relationship diagram

```mermaid
classDiagram
    class Client {
        Sends Request
        Receives Response
    }

    class Gateway {
        Facade
        -backends: Sequence~Backend~
        -strategy: BalancingStrategy
        -load: Map~backend name, in-flight count~
        +handle(request) Response
        -snapshot() Map
        -inc(name)
        -dec(name)
    }

    class Request {
        +prompt: str
        +client_id: str
    }

    class Response {
        +text: str
        +backend: str
        +latency_ms: float
        +meta: Mapping
    }

    class Backend {
        <<abstract>>
        +name: str
        +handle(request) Response
    }

    class VendorAAdapter {
        Adapter
        -client: VendorAClient
        +handle(request) Response
    }

    class VendorBAdapter {
        Adapter
        -client: VendorBClient
        +handle(request) Response
    }

    class VendorAClient {
        Simulated vendor SDK
        +complete(prompt) dict
    }

    class VendorBClient {
        Simulated vendor SDK
        +generate(messages) dict
    }

    class BalancingStrategy {
        <<abstract>>
        +select(backends, load) Backend
    }

    class RoundRobin {
        Strategy
        -counter: int
        +select(backends, load) Backend
    }

    class RandomChoice {
        Strategy
        -rng
        +select(backends, load) Backend
    }

    class LeastConnections {
        Strategy
        +select(backends, load) Backend
    }

    class BackendError {
        Shared backend failure
    }

    class NoBackendAvailable {
        Gateway failure after all attempts
    }

    class FactoryRegistry {
        Maps type names to constructors
    }

    class build_gateway {
        Reads config
        Creates strategy and adapters
        Returns Gateway
    }

    Client --> Gateway : calls handle(request)
    Gateway --> Request : accepts
    Gateway --> Response : returns
    Gateway o-- Backend : has configured backends
    Gateway --> BalancingStrategy : asks to select backend

    Backend <|-- VendorAAdapter
    Backend <|-- VendorBAdapter

    VendorAAdapter *-- VendorAClient : wraps
    VendorBAdapter *-- VendorBClient : wraps

    BalancingStrategy <|-- RoundRobin
    BalancingStrategy <|-- RandomChoice
    BalancingStrategy <|-- LeastConnections

    VendorAAdapter ..> BackendError : translates vendor errors
    VendorBAdapter ..> BackendError : translates vendor errors
    Gateway ..> BackendError : catches and retries another backend
    Gateway ..> NoBackendAvailable : raises if all fail

    FactoryRegistry ..> VendorAAdapter : vendor_a constructor
    FactoryRegistry ..> VendorBAdapter : vendor_b constructor
    FactoryRegistry ..> RoundRobin : round_robin constructor
    FactoryRegistry ..> RandomChoice : random constructor
    FactoryRegistry ..> LeastConnections : least_connections constructor
    build_gateway --> FactoryRegistry : looks up constructors
    build_gateway ..> Gateway : constructs
```

## Request flow diagram

Time moves from top to bottom. Solid arrows show calls or messages; dashed arrows show returned values or errors.

```mermaid
sequenceDiagram
    participant C as Client
    participant F as build_gateway / factory
    participant G as Gateway (Facade)
    participant S as Configured strategy
    participant A as Chosen backend adapter
    participant V as Simulated vendor client

    Note over F: Config selects round_robin, random, or least_connections
    F->>F: Look up constructor in registry
    F->>G: Construct Gateway(backends, strategy)

    C->>G: handle(Request)
    G->>S: select(available backends, current load)
    S-->>G: Return one Backend
    G->>G: Increment that backend's in-flight count
    G->>A: handle(Request)
    A->>A: Convert common Request to vendor API input
    A->>V: Call complete(prompt) or generate(messages)
    V->>V: Wait configured latency
    V->>V: Random draw compared with failure_rate

    alt Vendor call succeeds
        V-->>A: Vendor-specific response shape
        A->>A: Convert to common Response
        A-->>G: Response
        G->>G: Decrement in-flight count
        G-->>C: Return Response
    else Vendor call fails
        V-->>A: Vendor-specific exception
        A->>A: Translate exception to BackendError
        A-->>G: BackendError
        G->>G: Decrement in-flight count
        Note over G,S: Try a backend not yet attempted
        G->>S: select(remaining backends, current load)
        S-->>G: Return another Backend
        Note over G: Each backend is tried at most once
        G-->>C: Response if another backend succeeds
        G-->>C: NoBackendAvailable if all backends fail
    end
```

## Arrow key for the class diagram

- `-->` is a directed interaction or use. For example, the client calls the gateway.
- `<|--` is inheritance or implementation. The hollow triangle points to the parent abstraction: `VendorAAdapter` implements `Backend`.
- `o--` is aggregation: the gateway holds references to backend objects supplied to it.
- `*--` is composition: each adapter creates and holds its vendor client.
- `..>` is a dashed dependency, such as translating to or raising an error type.

In the sequence diagram, `->>` is a call or outgoing message and `-->>` is a return. The `alt` block shows the success and failure paths.

## Pattern and class roles

| Pattern or type | Classes | What it does |
|---|---|---|
| Adapter | `VendorAAdapter`, `VendorBAdapter` | Translates the shared `Backend` request and response contract to and from each vendor API. |
| Strategy | `RoundRobin`, `RandomChoice`, `LeastConnections` | Implements a backend selection rule. The factory configures which strategy the gateway uses; that strategy selects a backend for each request. |
| Facade | `Gateway` | Gives callers one `handle(request)` entry point and coordinates backend selection, load tracking, and failover. |
| Registry-based factory | `build_gateway`, `_BACKENDS`, `_STRATEGIES` | Looks up configured names and constructs the matching adapters and strategy. This is registry-based construction, not the classic GoF Factory Method with subclass-overridden creation methods. |

`Request` and `Response` are the common data types. `Backend` and `BalancingStrategy` are abstract contracts. `BackendError` is the normalized backend failure; `NoBackendAvailable` reports that all configured backends failed for the request. `VendorAClient` and `VendorBClient` simulate incompatible third-party SDK interfaces.
