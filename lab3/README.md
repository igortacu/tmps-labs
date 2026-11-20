# Lab 3: Structural patterns for the AI inference gateway

The supplied assignment requires a class-based language with explicit interfaces. This lab is a Java 17 Maven module in the existing gateway repository. It keeps the simulated inference domain and uses a Builder to assemble the structural patterns.

## Run

Requires JDK 17 or newer and Maven 3.9 or newer.

```sh
cd lab3
mvn test
mvn exec:java
```

`mvn test` runs 30 JUnit tests: 18 structural and 12 behavioral. `mvn exec:java` runs the earlier Composite/Decorator/Proxy demonstration.

## Selected structural patterns

The four selected patterns for Laboratory Work No. 2 are Adapter, Bridge, Decorator, and Facade. Their integrated demo uses a local HTTP server and runs with:

```sh
mvn exec:java -Dexec.mainClass=gateway.client.SelectedStructuralDemo
```

The execution path is `InferenceGatewayFacade → AuditDecorator → RetryDecorator → BackendGroup → InstructionMode → HttpBackendAdapter → JDK HttpClient`. See [REPORT.md](REPORT.md) for the diagrams, code excerpts, and tests. The earlier Composite and Proxy code remains as infrastructure used by the builder.

## Behavioral design patterns extension

The same Java module implements the five selected behavioral patterns from Laboratory Work No. 3: Interpreter, Memento, Observer, Strategy, and Visitor. Run its integrated console story with:

```sh
mvn test
mvn exec:java -Dexec.mainClass=gateway.client.BehavioralDemo
```

The behavioral implementation is in `src/main/java/gateway/behavioral/`, the runnable client is `gateway.client.BehavioralDemo`, and the pattern report is [BEHAVIORAL_REPORT.md](BEHAVIORAL_REPORT.md). It reuses `InferenceService`, `InferenceRequest`, `InferenceResponse`, and the composite backend nodes from the structural lab.

The behavioral report is in [BEHAVIORAL_REPORT.md](BEHAVIORAL_REPORT.md).
