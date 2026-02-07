---
name: Connector Framework Execution
overview: A detailed execution plan that breaks the Payment Connector Framework into milestones and implementable stories with clear dependencies and acceptance criteria.
todos: []
isProject: false
---

# Payment Connector Framework – Detailed Execution Plan

This plan splits the [Payment Connector Framework plan](payment-connector-framework-plan.md) into **milestones** (phases) and **stories** (discrete work items) with dependencies and acceptance criteria.

---

## Milestone overview

```mermaid
flowchart LR
  M1[M1 Foundation]
  M2[M2 Core Pipeline]
  M3[M3 First Transport]
  M4[M4 Observability]
  M5[M5 All Transports]
  M6[M6 Spring Integration]
  M7[M7 Replay and Advanced]
  M8[M8 Sample and Polish]
  M1 --> M2
  M2 --> M3
  M3 --> M4
  M4 --> M5
  M5 --> M6
  M6 --> M7
  M7 --> M8
```

---

## Milestone 1: Foundation (Build and project skeleton)

**Goal:** Reproducible Gradle 9.x multi-project build with Kotlin DSL, version catalog, and conventions. No application code yet.

| Story ID  | Story                            | Acceptance criteria                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| --------- | -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M1-S1** | Gradle root and settings         | `settings.gradle.kts` with `rootProject.name`; `include()` for all modules (connector-core, connector-journal, connector-transformation, connector-server-http, connector-server-grpc, connector-server-kafka, connector-server-jms, connector-client-http, connector-client-grpc, connector-client-kafka, connector-client-jms, connector-observability, connector-spring, connector-spring-boot-starter, connector-sample-app). Empty module directories and placeholder `build.gradle.kts` per module. |
| **M1-S2** | Version catalog                  | `gradle/libs.versions.toml` with aliases for Spring Boot 3.x, spring-jdbc, Resilience4j, OpenTelemetry, gRPC, Kafka, JMS, Spock, Groovy, JaCoCo. No JPA entries.                                                                                                                                                                                                                                                                                                                                          |
| **M1-S3** | Convention plugin (Java + tests) | Convention plugin (e.g. in `buildSrc` or `gradle/convention-plugins`) that applies Java plugin, sets Java 21, adds Spock/Groovy and JaCoCo to test scope, configures JaCoCo with 90% minimum and fail-on-below. All non-Boot modules use it.                                                                                                                                                                                                                                                              |
| **M1-S4** | Root build and Boot scope        | Root `build.gradle.kts` applies conventions to subprojects. Spring Boot and dependency-management applied only in connector-spring-boot-starter and connector-sample-app. `./gradlew build` succeeds (no source yet; tests empty).                                                                                                                                                                                                                                                                        |


**Exit criteria:** `./gradlew build` and `./gradlew jacocoTestCoverageVerification` run; structure matches plan; Java 21 and 90% coverage enforced.

---

## Milestone 2: Core pipeline (Message model and transport SPI)

**Goal:** connector-core with canonical message, correlation ID, transport SPI, and a no-op/in-memory journal interface. No RDBMS or Spring yet.

| Story ID  | Story                               | Acceptance criteria                                                                                                                                                                                                                                           |
| --------- | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M2-S1** | ConnectorMessage and correlation ID | Record/class `ConnectorMessage`: correlationId, transportType, payload (e.g. byte[] or generic), headers Map, timestamp. Utility to generate or extract correlation ID (e.g. from headers). Immutable where possible.                                         |
| **M2-S2** | Transport SPI                       | Interfaces: `InboundTransport` (start, stop, isRunning, setMessageHandler(MessageHandler)); `OutboundTransport` (send(ConnectorMessage, Map) returning CompletableFuture of SendResult); `MessageHandler` (handle(ConnectorMessage)). No implementations yet. |
| **M2-S3** | Journal SPI                         | Interface for "append request" and "update response" (e.g. JournalWriter with appendRequest, updateResponse) and optional "get by correlation ID" for replay. In-memory or no-op implementation for tests.                                                    |
| **M2-S4** | Core tests and coverage             | Spock unit specs for ConnectorMessage, correlation ID, and SPI contracts (mock handlers/transports). JaCoCo ≥ 90% for connector-core.                                                                                                                         |


**Exit criteria:** connector-core JAR builds; SPIs and message model are stable; tests pass and coverage ≥ 90%.

---

## Milestone 3: Journal and transformation

**Goal:** connector-journal with Spring JDBC and native SQL; connector-transformation with pipeline and converter registry.

| Story ID  | Story                                | Acceptance criteria                                                                                                                                                                                                                                                           |
| --------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M3-S1** | Journal DDL and schema               | DDL scripts (e.g. `schema.sql` or Flyway) for `connector_journal` (id, correlation_id, direction, transport, payload_type, payload_blob, headers_json, status, created_at, processed_at, error_message) and optional `connector_hold`. Indexes on correlation_id, created_at. |
| **M3-S2** | Journal DAO (Spring JDBC)            | DAO using JdbcTemplate/NamedParameterJdbcTemplate only; parameterised INSERT/UPDATE/SELECT; RowMapper for reads. Implements JournalWriter (and optional read-by-correlationId). No JPA; dependency is spring-jdbc only.                                                       |
| **M3-S3** | Transformation pipeline and registry | Pipeline: receive → input convert → journal request → transform → output convert → send → journal response. Registry for registering InputConverter and OutputConverter by transport/content-type. One example converter (e.g. pass-through or JSON → internal → JSON).       |
| **M3-S4** | Journal and transformation tests     | Spock unit specs for DAO (with H2 or Testcontainers); Spock unit specs for pipeline and registry with stub converters. Coverage ≥ 90% for connector-journal and connector-transformation.                                                                                     |


**Exit criteria:** Request/response can be journalled via JDBC; pipeline runs with registered converters; no JPA; tests and coverage pass.

---

## Milestone 4: First transport (HTTP server and client)

**Goal:** One server (HTTP) and one client (HTTP) with start/stop, health, and resilience; wired to core pipeline.

| Story ID  | Story                                   | Acceptance criteria                                                                                                                                                                                         |
| --------- | --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M4-S1** | HTTP server (InboundTransport)          | REST controller or WebFlux endpoint that accepts payload, builds ConnectorMessage (with correlation ID from header or generated), invokes MessageHandler. Controllable wrapper: start/stop; isRunning.      |
| **M4-S2** | HTTP server Actuator control and health | Custom Actuator endpoint to start/stop HTTP listener by name; health indicator that reports UP when listener is started (and optionally connected). Exposed under actuator base path.                       |
| **M4-S3** | HTTP client (OutboundTransport)         | Client that sends ConnectorMessage to configurable URL; returns CompletableFuture of SendResult. Optional: propagate correlation ID in header.                                                              |
| **M4-S4** | HTTP client resilience                  | Resilience4j: Retry (with backoff), Bulkhead (semaphore or pool), optional RateLimiter. Configurable via properties. Load shedding: fail fast or bounded queue when limit reached; expose "shedded" metric. |
| **M4-S5** | HTTP transport tests                    | Spock unit specs (mock handler/client); integration test with WireMock or similar. Start/stop and health verified. Coverage ≥ 90% for HTTP server and client modules.                                       |


**Exit criteria:** HTTP in and out work with pipeline; start/stop and health visible in Actuator; resilience on client; tests and coverage pass.

---

## Milestone 5: Observability

**Goal:** OpenTelemetry tracing and metrics across pipeline and HTTP transport; correlation_id in spans.

| Story ID  | Story               | Acceptance criteria                                                                                                                                                                                                        |
| --------- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M5-S1** | Tracing in pipeline | One span per incoming request; correlation_id as span attribute; child spans for "transform" and "send"; context propagated to HTTP client (headers). Use OTel API or Spring Boot OTel starter.                            |
| **M5-S2** | Metrics in pipeline | Counters: received/sent/failed per transport; throughput (messages/sec); histograms: latency (receive→ack, transform, send). Classify by transport. Optional: connector-observability module that other modules depend on. |
| **M5-S3** | Observability tests | Unit specs with mocked OTel/meter provider; integration test that asserts span attributes and metric counts. Coverage for new observability code ≥ 90%.                                                                    |


**Exit criteria:** Traces and metrics available for HTTP flow; correlation_id in traces; tests pass.

---

## Milestone 6: Remaining transports (gRPC, Kafka, JMS)

**Goal:** All four server types and all four client types implemented with same patterns as HTTP (start/stop, health, resilience).

| Story ID  | Story                   | Acceptance criteria                                                                                                                                                                                        |
| --------- | ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M6-S1** | gRPC server and client  | gRPC service implements InboundTransport; controllable start/stop; health indicator. gRPC client implements OutboundTransport with Resilience4j (retry, bulkhead, rate limit). Correlation ID in metadata. |
| **M6-S2** | Kafka server and client | Kafka listener container implements InboundTransport; start/stop via container lifecycle; health indicator. Kafka producer client implements OutboundTransport with resilience. Correlation ID in headers. |
| **M6-S3** | JMS server and client   | JMS listener container implements InboundTransport; start/stop and health. JMS client (send to queue/topic) implements OutboundTransport with resilience.                                                  |
| **M6-S4** | Transport tests         | Spock unit and integration tests per transport (e.g. embedded Kafka, in-memory JMS or Testcontainers). Each transport module ≥ 90% coverage.                                                               |


**Exit criteria:** All four server and four client types work; each has Actuator control/health and client resilience; tests pass.

---

## Milestone 7: Spring integration and starter

**Goal:** connector-spring and connector-spring-boot-starter with auto-configuration and conditional beans.

| Story ID  | Story                             | Acceptance criteria                                                                                                                                                                                                              |
| --------- | --------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M7-S1** | connector-spring                  | @Configuration beans for core pipeline, journal (JdbcTemplate + DataSource), transformation registry. No transport-specific beans; minimal Spring dependency in API.                                                             |
| **M7-S2** | Auto-configuration and conditions | Starter depends on core, journal, transformation, connector-spring. Auto-configuration for each transport using @ConditionalOnClass and @ConditionalOnProperty; register only when dependency present (e.g. Kafka on classpath). |
| **M7-S3** | Actuator aggregation              | Single ConnectorControlEndpoint listing all registered transports and allowing start/stop by name; composite health indicator "connectorServers" aggregating per-transport health.                                               |
| **M7-S4** | Starter and spring tests          | @SpringBootTest specs that enable only selected transports and assert bean presence and endpoint behaviour. Coverage ≥ 90% for connector-spring and starter.                                                                     |


**Exit criteria:** Adding the starter + one transport dependency yields working server/client and Actuator; conditional loading works; tests pass.

---

## Milestone 8: Replay, hold/release, micro-batching

**Goal:** Replay and hold/release API; micro-batching and back pressure for Kafka/JMS.

| Story ID  | Story                      | Acceptance criteria                                                                                                                                                                                                |
| --------- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **M8-S1** | Replay API                 | Endpoint or service "replay by correlation_id": read request from journal (native SELECT), re-run through transformation and OutboundTransport. Idempotency considerations documented or optional idempotency key. |
| **M8-S2** | Hold and release           | Mark journal entry as held (UPDATE); connector_hold table or status field. Scheduled job or endpoint "release" processes held items after held_until or on demand.                                                 |
| **M8-S3** | Micro-batching (Kafka/JMS) | Bounded buffer (size + optional time window); batch of messages processed through pipeline; back pressure: block or pause consumer when buffer full. Virtual threads or small pool for batch processing.           |
| **M8-S4** | Replay and batching tests  | Spock tests for replay and hold/release (with journal DAO); tests for batch behaviour and back pressure. Coverage maintained ≥ 90%.                                                                                |


**Exit criteria:** Replay and hold/release work end-to-end; Kafka/JMS consumers use micro-batching and back pressure; tests pass.

---

## Milestone 9: Sample app and documentation

**Goal:** End-to-end sample app and CI/documentation polish.

| Story ID  | Story                | Acceptance criteria                                                                                                                                                                                              |
| --------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **M9-S1** | connector-sample-app | Spring Boot app: HTTP server → transform → Kafka client (or similar); journalling on request/response; replay and health/control via Actuator. Virtual threads and application.properties documented.            |
| **M9-S2** | CI and coverage gate | CI job runs `./gradlew build` and `jacocoTestCoverageVerification`; merge blocked if tests fail or coverage < 90%. Optional: aggregated JaCoCo report.                                                            |
| **M9-S3** | README and docs      | README: project overview, module layout, how to add starter and choose transports, configuration highlights (connector.servers.*, connector.clients.*), link to plan. No JPA; Spring JDBC and native SQL stated. |


**Exit criteria:** Sample app runs end-to-end; CI enforces tests and 90% coverage; README and docs align with plan.

---

## Story dependency summary

- **M1** must complete before any code in other modules.
- **M2** (core) is required for M3 (journal + transformation), M4 (HTTP), and all later milestones.
- **M3** (journal + transformation) is required for M4 (first transport) and replay in M8.
- **M4** (HTTP) is required for M5 (observability on a real transport) and M6 (same patterns for other transports).
- **M5** can run in parallel with or after M4; M6 depends on M4 (and optionally M5 for metrics in new transports).
- **M7** (starter) depends on at least M4 (one transport) and ideally M6 (all transports).
- **M8** (replay, batching) depends on M3 and M6 (journal + Kafka/JMS).
- **M9** (sample, CI, docs) depends on M7 and M8.

---

## Suggested story sizing (relative)

- **Small (1–2 days):** M1-S1, M1-S2, M2-S1, M2-S2, M2-S3, M5-S1, M5-S2, M7-S1, M9-S2, M9-S3.
- **Medium (3–5 days):** M1-S3, M1-S4, M2-S4, M3-S1, M3-S2, M3-S3, M3-S4, M4-S1, M4-S2, M4-S3, M4-S4, M4-S5, M5-S3, M7-S2, M7-S3, M7-S4, M8-S1, M8-S2, M8-S4, M9-S1.
- **Large (5–8 days):** M6-S1, M6-S2, M6-S3, M6-S4 (each transport can be one story or split), M8-S3 (micro-batching and back pressure).

Adjust sizing to team velocity; Spock tests and 90% coverage are part of each story's definition of done.
