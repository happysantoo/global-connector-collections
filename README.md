# Payment Connector Framework (global-connector-collections)

Spring Boot 3.x / Java 21 connector framework for processing payment messages across four transport types: **HTTP**, **gRPC**, **Kafka**, and **JMS**.

## Features

- **Four server types**: HTTP REST, gRPC, Kafka listener, JMS listener — each with start/stop and health checks via Actuator.
- **Four client types**: HTTP, gRPC, Kafka producer, JMS client — with resilience (retries, bulkheading, load shedding).
- **Unified pipeline**: Canonical `ConnectorMessage` with correlation ID; transformation registry for input/output converters; journalling (request/response) via **Spring JDBC and native SQL** (no JPA).
- **Observability**: OpenTelemetry tracing and metrics; correlation ID propagated through the pipeline.
- **Replay**: Replay by correlation ID; optional hold and release for delayed processing.
- **Build**: Gradle 8.x, Kotlin DSL, Java 21, Spock tests, 90% JaCoCo coverage.

## Module layout

| Module | Description |
|--------|-------------|
| `connector-core` | Message model, correlation ID, transport SPI (Inbound/Outbound), journal SPI |
| `connector-journal` | Journal persistence via Spring JDBC and native SQL (DDL + DAO) |
| `connector-transformation` | Pipeline and converter registry |
| `connector-server-http`, `connector-server-grpc`, `connector-server-kafka`, `connector-server-jms` | Inbound transports with start/stop and health |
| `connector-client-http`, `connector-client-grpc`, `connector-client-kafka`, `connector-client-jms` | Outbound transports with resilience |
| `connector-observability` | OpenTelemetry tracing and metrics |
| `connector-spring` | Spring configuration for core, journal, transformation |
| `connector-spring-boot-starter` | Auto-configuration and Actuator endpoints |
| `connector-sample-app` | Sample Spring Boot app (HTTP → transform → Kafka) |
| `connector-demo-jms-kafka` | **Demo: JMS → pipeline → Kafka** with observability, resilience, replay, hold/release, health/control |

## Quick start

1. **Build**: `./gradlew build`
2. **Coverage**: `./gradlew jacocoTestCoverageVerification` (90% minimum)
3. **Run sample app**: `./gradlew :connector-sample-app:bootRun`
4. **Run JMS→Kafka demo** (proves library with all NFRs): `./gradlew :connector-demo-jms-kafka:bootRun` — see [connector-demo-jms-kafka/README.md](connector-demo-jms-kafka/README.md)

## Configuration

- **Connector servers**: `connector.servers.<transport>.enabled` (e.g. `connector.servers.kafka.enabled=true`)
- **Connector clients**: `connector.clients.<transport>.url` (or transport-specific props)
- **Actuator**: Expose `connector` and `health` for control and health: `management.endpoints.web.exposure.include=connector,health`
- **Virtual threads**: `spring.threads.virtual.enabled=true` for IO-bound scaling
- **Journal**: DataSource must be provided; schema is applied via `schema.sql` (or Flyway/Liquibase). **No JPA** — Spring JDBC and native SQL only.

## Design and execution plans

Detailed design and milestone/story breakdown are in:

- [documents/design-plans/payment-connector-framework-plan.md](documents/design-plans/payment-connector-framework-plan.md)
- [documents/design-plans/connector-framework-execution-plan.md](documents/design-plans/connector-framework-execution-plan.md)

## License

See project license file.
