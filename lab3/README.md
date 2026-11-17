# Lab 3: Structural patterns for the AI inference gateway

The supplied assignment requires a class-based language with explicit interfaces. This lab is a Java 17 Maven module in the existing gateway repository. It keeps the simulated inference domain and uses a Builder to assemble the structural patterns.

## Run

Requires JDK 17 or newer and Maven 3.9 or newer.

```sh
cd lab3
mvn test
mvn exec:java
```

`mvn test` runs 11 JUnit tests. `mvn exec:java` prints an authorized response, an audit event, and a blocked request. The execution path is `AuthorizationProxy → AuditDecorator → RetryDecorator → BackendGroup → SimulatedBackend`.

The full report, diagrams, source excerpts, and test evidence are in [REPORT.md](REPORT.md).
