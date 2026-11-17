# Lab 3: Structural patterns for the AI inference gateway

The attached structural-pattern assignment requires a class-based language with explicit interfaces, so this lab is a TypeScript module beside the existing Python gateway. It carries forward the same simulated inference domain and uses a Builder to assemble the new pattern chain.

## Run

Requires Node.js 22 or newer and npm.

```sh
cd lab3
npm ci
npm test
npm run demo
```

`npm test` compiles TypeScript and runs 11 unit/integration tests. `npm run demo` prints an authorized response, an audit event, and a blocked request. The runnable path is `AuthorizationProxy → AuditDecorator → RetryDecorator → BackendGroup → SimulatedBackend`.

The full report, diagrams, code excerpts, and test evidence are in [REPORT.md](REPORT.md).
