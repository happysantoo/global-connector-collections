# Connector Demo: JMS → Kafka

This application demonstrates the **Payment Connector Framework** with **JMS as inbound** and **Kafka as outbound**, plus all supported non-functional requirements.

**Detailed flow (transformation + linking bridge):** [documents/DEMO_JMS_KAFKA_FLOW.md](../documents/DEMO_JMS_KAFKA_FLOW.md) — flow diagrams, where transformation runs, and where the JMS→Kafka bridge is wired.

## Flow

```
JMS (queue: connector-in) → Pipeline (input convert → journal → output convert → send) → Kafka (topic: connector-out)
```

- **Inbound:** Messages consumed from JMS queue `connector-in` (embedded Artemis).
- **Transformation:** **Input converter** (JMS → internal: normalize payload); **output converter** (internal → Kafka: add `[JMS→Kafka] ` prefix). Registered in `JmsToKafkaTransformation`; invoked inside `ConnectorPipeline.process()`.
- **Pipeline:** Input convert → journal request → output convert → send → journal response. Optional tracing and metrics.
- **Outbound:** Kafka producer with **Resilience4j** (Retry, Bulkhead, RateLimiter) and virtual threads.

## Non-Functional Requirements Demonstrated

| Requirement | How |
|-------------|-----|
| **Observability** | `ConnectorMetricsRegistry` and optional `ConnectorTracing` in the pipeline; `GET /connector/metrics` for per-transport counts. |
| **Resilience** | `ResilientKafkaOutboundTransport` (Retry, Bulkhead, RateLimiter). Toggle with `connector.demo.resilience.enabled=true|false`. |
| **Replay** | `POST /connector/replay/{correlationId}` reads from journal and re-sends to Kafka. |
| **Hold/Release** | `POST /connector/hold/{correlationId}`, `POST /connector/hold/{correlationId}/release`, `GET /connector/hold/due`, `POST /connector/hold/release-due`. |
| **Health & Control** | Actuator: `GET /actuator/connector` (list transports, start/stop), `POST /actuator/connector` (control), `GET /actuator/health` (composite connectorServers). |
| **Journaling** | Every request/response is written to `connector_journal` (H2). |
| **Virtual threads** | `spring.threads.virtual.enabled=true` for outbound executor. |

## Prerequisites

- **Java 21**
- **Kafka** running (e.g. localhost:9092). JMS uses **embedded Artemis** (no external broker required).

## Run

```bash
# From project root
./gradlew :connector-demo-jms-kafka:bootRun
```

Or with explicit Kafka bootstrap:

```bash
./gradlew :connector-demo-jms-kafka:bootRun --args='--spring.kafka.bootstrap-servers=localhost:9092'
```

App runs on **port 8081**.

## Test the Flow

1. **Send a message into JMS** (so the pipeline processes it and sends to Kafka):

   ```bash
   curl -X POST http://localhost:8081/demo/send -H "Content-Type: application/octet-stream" -d "hello from JMS"
   ```

   This uses the demo’s helper endpoint that enqueues to `connector-in`. The pipeline will consume it, journal it, and send to Kafka topic `connector-out`.

2. **Check metrics**

   ```bash
   curl http://localhost:8081/connector/metrics
   ```

3. **Check connector state and health**

   ```bash
   curl http://localhost:8081/actuator/connector
   curl http://localhost:8081/actuator/health
   ```

4. **Replay by correlation ID** (use a correlation ID from the journal or from logs):

   ```bash
   curl -X POST http://localhost:8081/connector/replay/YOUR_CORRELATION_ID
   ```

5. **Hold and release**

   ```bash
   curl -X POST http://localhost:8081/connector/hold/corr-123 -H "Content-Type: application/json" -d '{"heldUntil":"2026-12-31T00:00:00Z","reason":"manual"}'
   curl -X POST http://localhost:8081/connector/hold/corr-123/release
   curl http://localhost:8081/connector/hold/due
   curl -X POST http://localhost:8081/connector/hold/release-due
   ```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `connector.servers.http.enabled` | (true in starter) | Set to `false` in this demo so only JMS is used for inbound. |
| `connector.servers.jms.enabled` | true | Enable JMS inbound. |
| `spring.artemis.embedded.queues` | connector-in | Pre-create JMS queue. |
| `connector.demo.kafka.topic` | connector-out | Kafka output topic. |
| `connector.demo.resilience.enabled` | true | Wrap Kafka outbound with Resilience4j. |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Kafka brokers. |

## Module Dependencies

- **connector-spring-boot-starter** (core, journal, transformation, observability, actuator)
- **connector-server-jms** (JMS inbound transport, health, TransportRegistration)
- **connector-client-kafka** (Kafka outbound + ResilientKafkaOutboundTransport)
- **connector-journal** (JdbcJournalWriter, ReplayService, HoldReleaseService)
- **spring-boot-starter-artemis** (embedded JMS broker)
- **resilience4j-spring-boot3** (Retry, Bulkhead, RateLimiter)
