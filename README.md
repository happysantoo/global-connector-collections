# Payment Connector Framework (global-connector-collections)

A **Spring Boot 3.x / Java 21** connector framework for processing payment (or any) messages across **HTTP**, **gRPC**, **Kafka**, and **JMS**. It provides a unified pipeline with journalling, transformation, observability, resilience, and replay—so you can ingest from any transport, transform, and publish to any other with a single abstraction.

---

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Module layout](#module-layout)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Routing: multiple inputs and outputs](#routing-multiple-inputs-and-outputs)
- [Sample and demo applications](#sample-and-demo-applications)
- [Documentation](#documentation)
- [Build and CI](#build-and-ci)
- [License](#license)

---

## Overview

The framework lets you:

- **Consume** messages from one or more of: HTTP REST, gRPC, Kafka, JMS.
- **Transform** them via a configurable input/output conversion registry (e.g. JMS format → internal → Kafka format).
- **Journal** every request and response (Spring JDBC, native SQL, no JPA).
- **Publish** to one outbound transport per pipeline (Kafka, JMS, HTTP, gRPC) with optional Resilience4j (retry, bulkhead, rate limit).
- **Replay** by correlation ID and use **hold/release** for delayed processing.
- **Control and monitor** all transports via Spring Boot Actuator (start/stop, composite health).

You can run **different routes** in the same process (e.g. JMS→Kafka, HTTP→JMS) by wiring each inbound to the appropriate pipeline. See [Routing](#routing-multiple-inputs-and-outputs).

---

## Features

| Area | What you get |
|------|----------------|
| **Transports** | Four server types (HTTP, gRPC, Kafka, JMS) and four client types; each with start/stop and health; optional micro-batching (BatchBuffer) for Kafka/JMS consumers. |
| **Pipeline** | Single canonical `ConnectorMessage` (correlation ID, payload, headers); input conversion (per transport) → journal → output conversion (per destination) → send. |
| **Transformation** | `MessageConversionRegistry`: register input converters (e.g. JMS → internal) and output converters (internal → Kafka); pipeline invokes them automatically. |
| **Journal** | Request/response persisted via Spring JDBC and native SQL; tables `connector_journal` and `connector_hold`; no JPA. |
| **Observability** | Optional OpenTelemetry tracing (one span per request, correlation_id attribute) and metrics (received/sent/failed per transport); wired in pipeline when beans present. |
| **Resilience** | Resilient wrappers for each outbound: Retry, Bulkhead, RateLimiter (Resilience4j); virtual-thread executor supported. |
| **Replay** | Replay by correlation ID from journal through pipeline to outbound; optional hold/release (mark held, release on schedule or on demand). |
| **Actuator** | Single `connector` endpoint listing all transports and allowing start/stop by name; composite health indicator `connectorServers`. |
| **Build** | Gradle 8.x, Kotlin DSL, Java 21, Spock tests, JaCoCo 90% minimum. |

---

## Architecture

High-level flow for one route (e.g. JMS → Kafka):

```
  Inbound (e.g. JMS)     Linking bridge              Pipeline                    Outbound (e.g. Kafka)
  ─────────────────      ─────────────               ────────                    ─────────────────────
  Queue/HTTP/Kafka  →   MessageHandler  →   input convert → journal  →   output convert  →  send  →  Topic/Queue/HTTP
                              ↑                    ↑                              ↑
                        setMessageHandler()   MessageConversionRegistry      OutboundTransport
```

- **Linking bridge:** Each inbound transport has a `MessageHandler`; the handler calls `ConnectorPipeline.process(message, options)`. The pipeline uses one `OutboundTransport` and one output converter key.
- **Transformation:** Input converter is selected by `message.transportType()`; output converter by the pipeline’s `outputTransport` name. Both come from `MessageConversionRegistry`.

For **multiple routes** (e.g. JMS→Kafka and HTTP→JMS), you define multiple pipelines and wire each inbound to the handler that calls the right pipeline. See [documents/MULTI_INPUT_OUTPUT_DESIGN.md](documents/MULTI_INPUT_OUTPUT_DESIGN.md).

---

## Module layout

| Module | Description |
|--------|-------------|
| **connector-core** | `ConnectorMessage`, correlation ID, transport SPI (`InboundTransport`, `OutboundTransport`, `MessageHandler`), journal SPI (`JournalWriter`), `BatchBuffer` for micro-batching, `TransportRegistration` for Actuator. |
| **connector-journal** | DDL (`connector_journal`, `connector_hold`), `JdbcJournalWriter`, `ReplayService`, `HoldReleaseService` / `JdbcHoldReleaseService`. Spring JDBC only. |
| **connector-transformation** | `ConnectorPipeline` (input convert → journal → output convert → send), `MessageConversionRegistry`, `InputConverter` / `OutputConverter`. |
| **connector-observability** | `ConnectorTracing` (OTel span), `ConnectorMetrics` and `ConnectorMetricsRegistry` (per-transport counters). |
| **connector-server-http** | `HttpInboundTransport`, REST controller, health, `TransportRegistration`. |
| **connector-server-grpc** | `GrpcInboundTransport`, health, `TransportRegistration`; wire to your gRPC service. |
| **connector-server-kafka** | `KafkaInboundTransport` (optional `BatchBuffer`), health, `TransportRegistration`. |
| **connector-server-jms** | `JmsInboundTransport` (optional `BatchBuffer`), health, `TransportRegistration`. |
| **connector-client-http** | `HttpOutboundTransport`, `ResilientHttpOutboundTransport`. |
| **connector-client-grpc** | `GrpcOutboundTransport`, `ResilientGrpcOutboundTransport`. |
| **connector-client-kafka** | `KafkaOutboundTransport`, `ResilientKafkaOutboundTransport`. |
| **connector-client-jms** | `JmsOutboundTransport`, `ResilientJmsOutboundTransport`. |
| **connector-spring** | `ConnectorSpringConfiguration`: registry, journal beans, optional `ConnectorTracing` / `ConnectorMetricsRegistry`. |
| **connector-spring-boot-starter** | Auto-configuration and Actuator: `ConnectorControlEndpoint`, composite health; depends on spring and HTTP server by default; add Kafka/JMS/gRPC modules to get those transports. |
| **connector-sample-app** | Minimal sample: HTTP → pipeline → Kafka, journalling, replay, Actuator. |
| **connector-demo-jms-kafka** | Full demo: JMS → pipeline (with explicit input/output transformation) → Kafka; observability, resilience, replay, hold/release, health/control. |

---

## Quick start

**Prerequisites:** Java 21, Kafka (for sample/demo that use Kafka outbound).

```bash
# Clone and enter the project
git clone https://github.com/happysantoo/global-collector-collections.git
cd global-collector-collections

# Build everything (tests + coverage)
./gradlew build jacocoTestCoverageVerification

# Run the HTTP → Kafka sample (needs Kafka on localhost:9092)
./gradlew :connector-sample-app:bootRun

# Run the JMS → Kafka demo (embedded JMS; needs Kafka on localhost:9092)
./gradlew :connector-demo-jms-kafka:bootRun
```

- **Sample app** runs on port **8080**; **demo app** on port **8081**.
- Demo includes a helper endpoint: `POST http://localhost:8081/demo/send` with body to inject a message into JMS and see it flow to Kafka.

---

## Configuration

| What | Property / note |
|------|------------------|
| **Enable/disable transports** | `connector.servers.http.enabled`, `connector.servers.kafka.enabled`, `connector.servers.jms.enabled`, `connector.servers.grpc.enabled` (defaults typically `true` where applicable). |
| **Actuator** | `management.endpoints.web.exposure.include=connector,health` (and `info` if desired). |
| **Virtual threads** | `spring.threads.virtual.enabled=true` for outbound executors. |
| **Journal** | Provide a `DataSource`; schema is applied via `schema.sql` (or your own DDL). **No JPA** — Spring JDBC and native SQL only. |
| **Kafka** | `spring.kafka.bootstrap-servers`, topic names in your pipeline options or config. |
| **JMS** | Embedded Artemis: `spring.artemis.embedded.queues=connector-in`; or point to external broker. |

---

## Routing: multiple inputs and outputs

- **Multiple inputs** in the same process are supported: register HTTP, JMS, Kafka, gRPC; wire each to a `MessageHandler` (same or different). Input conversion is keyed by `message.transportType()`.
- **Different input/transform/output combinations** are supported: use one pipeline per output (e.g. pipeline to Kafka, pipeline to JMS); wire each inbound to the handler that calls the pipeline you want; register input/output converters per transport in the shared registry. See [documents/MULTI_INPUT_OUTPUT_DESIGN.md](documents/MULTI_INPUT_OUTPUT_DESIGN.md).
- **Single pipeline, multiple outputs** (fan-out) is not built in; the doc above describes a **CompositeOutboundTransport** option and pipeline extensions to support it.

---

## Sample and demo applications

| App | Flow | Purpose |
|-----|------|--------|
| **connector-sample-app** | HTTP → pipeline → Kafka | Minimal wiring: one inbound, one outbound, journal and replay. |
| **connector-demo-jms-kafka** | JMS → pipeline (transform) → Kafka | Demonstrates transformation (input/output converters), resilience, replay, hold/release, metrics, Actuator. |

Demo README: [connector-demo-jms-kafka/README.md](connector-demo-jms-kafka/README.md).  
Flow and bridge/transformation details: [documents/DEMO_JMS_KAFKA_FLOW.md](documents/DEMO_JMS_KAFKA_FLOW.md).

---

## Documentation

| Document | Description |
|----------|-------------|
| [documents/design-plans/payment-connector-framework-plan.md](documents/design-plans/payment-connector-framework-plan.md) | High-level plan: tech stack, architecture, modules. |
| [documents/design-plans/connector-framework-execution-plan.md](documents/design-plans/connector-framework-execution-plan.md) | Milestones and stories with acceptance criteria. |
| [documents/DEMO_JMS_KAFKA_FLOW.md](documents/DEMO_JMS_KAFKA_FLOW.md) | JMS→Kafka flow: where transformation runs, where the linking bridge is wired (with diagrams). |
| [documents/MULTI_INPUT_OUTPUT_DESIGN.md](documents/MULTI_INPUT_OUTPUT_DESIGN.md) | Multiple inputs/outputs: what’s supported, bottlenecks, and options (composite outbound, pipeline multi-out). |
| [documents/COMPLETENESS_REPORT.md](documents/COMPLETENESS_REPORT.md) | Completeness vs execution plan. |
| [documents/README.md](documents/README.md) | Index of all docs. |

---

## Build and CI

- **Build:** `./gradlew build`
- **Coverage (90% minimum):** `./gradlew jacocoTestCoverageVerification`
- **CI:** GitHub Actions runs `./gradlew build jacocoTestCoverageVerification` (see [.github/workflows/ci.yml](.github/workflows/ci.yml)).

Gradle 8.x, Kotlin DSL, Java 21; version catalog in `gradle/libs.versions.toml`; convention plugin in `buildSrc` for Java, JaCoCo, Spock.

---

## License

Apache License 2.0. See [LICENSE](LICENSE) in the repository.
