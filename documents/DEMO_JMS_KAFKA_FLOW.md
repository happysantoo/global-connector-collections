# JMS → Kafka Demo: Flow and Architecture

This document describes where **transformation** (input/output message format translation) happens and where the **linking bridge** (JMS consumption → pipeline → Kafka publication) is wired. It applies to the **connector-demo-jms-kafka** application.

---

## 1. High-level flow

```
┌─────────────┐     ┌──────────────────────────────────────────────────────────┐     ┌─────────────┐
│  JMS Queue  │────▶│  Connector Pipeline (transform, journal, observability)  │────▶│   Kafka     │
│ connector-in│     │  + Linking bridge (MessageHandler → Pipeline → Outbound)  │     │ connector-out│
└─────────────┘     └──────────────────────────────────────────────────────────┘     └─────────────┘
```

End-to-end:

1. A message is **consumed from JMS** (queue `connector-in`).
2. It is turned into a **ConnectorMessage** and passed to the **pipeline** via the **MessageHandler** (the bridge).
3. The pipeline runs **input conversion** (JMS format → internal), **journals** the request, runs **output conversion** (internal → Kafka format), then **sends** to Kafka via the outbound transport.
4. The **response** (success/failure) is journalled.

---

## 2. Flow diagram (detailed)

```mermaid
sequenceDiagram
    participant JMS as JMS Queue (connector-in)
    participant Container as DefaultMessageListenerContainer
    participant JMSTransport as JmsInboundTransport
    participant Handler as MessageHandler (jmsToPipelineHandler)
    participant Pipeline as ConnectorPipeline
    participant Registry as MessageConversionRegistry
    participant Journal as JournalWriter
    participant Outbound as OutboundTransport (Kafka)
    participant Kafka as Kafka Topic (connector-out)

    JMS->>Container: message
    Container->>JMSTransport: onMessage(message)
    JMSTransport->>JMSTransport: build ConnectorMessage (correlationId, payload, headers)
    JMSTransport->>Handler: handle(connectorMessage)
    Handler->>Pipeline: process(connectorMessage, sendOptions)

    Note over Pipeline: Observability: metrics + optional tracing
    Pipeline->>Registry: getInputConverter("jms")
    Registry-->>Pipeline: InputConverter (JMS → internal)
    Pipeline->>Pipeline: internal = convert(message)
    Pipeline->>Journal: appendRequest(internal)
    Pipeline->>Registry: getOutputConverter("kafka")
    Registry-->>Pipeline: OutputConverter (internal → Kafka format)
    Pipeline->>Pipeline: toSend = convert(internal)
    Pipeline->>Outbound: send(toSend, options)
    Outbound->>Kafka: produce record
    Kafka-->>Outbound: ack
    Outbound-->>Pipeline: CompletableFuture<SendResult>
    Pipeline->>Journal: updateResponse(correlationId, status)
```

---

## 3. Where is the linking bridge?

The **bridge** is the chain that connects “JMS message consumed” to “Kafka message published”.

| Step | Component | Role |
|------|-----------|------|
| 1 | **DefaultMessageListenerContainer** | Listens to JMS queue `connector-in`; on each message calls `MessageListener.onMessage()`. |
| 2 | **JmsInboundTransport** | Implements `MessageListener`. Builds a **ConnectorMessage** from the JMS message (payload, headers, correlation ID) and calls **MessageHandler.handle(message)**. |
| 3 | **MessageHandler (jmsToPipelineHandler)** | The bridge bean: `message -> pipeline.process(message, Map.of("topic", "connector-out"))`. Connects inbound to pipeline. |
| 4 | **ConnectorPipeline** | Runs conversion (input → journal → output), then calls **OutboundTransport.send(toSend, options)**. |
| 5 | **OutboundTransport (Kafka)** | Implemented by **KafkaOutboundTransport** (wrapped by **ResilientKafkaOutboundTransport**). Sends **ConnectorMessage** payload to Kafka topic `connector-out`. |

**Code locations (demo):**

- **Bridge wiring:** `DemoJmsKafkaConfiguration`:
  - `jmsListenerContainer(..., JmsInboundTransport jmsInboundTransport)` — container’s listener is the JMS transport.
  - `jmsToPipelineHandler(ConnectorPipeline pipeline)` — handler that calls `pipeline.process(...)`.
  - `WireJmsHandler` — sets `jmsInboundTransport.setMessageHandler(jmsToPipelineHandler)` so every JMS message goes into the pipeline.
- **Pipeline → Kafka:** same config: `connectorPipeline(...)` receives `OutboundTransport kafkaOutboundTransport` and uses it in `process()` to send.

So: **JMS → JmsInboundTransport → MessageHandler → ConnectorPipeline → OutboundTransport (Kafka)** is the linking bridge.

---

## 4. Where is the transformation (input/output format translation)?

Transformation is **input conversion** (JMS format → internal) and **output conversion** (internal → Kafka format). It is performed **inside ConnectorPipeline** using **MessageConversionRegistry**.

### 4.1 Pipeline flow (with conversion)

```mermaid
flowchart LR
    A[ConnectorMessage from JMS] --> B{Input converter for 'jms'?}
    B -->|Yes| C[internal = convert]
    B -->|No| D[internal = message]
    C --> E[Journal appendRequest]
    D --> E
    E --> F{Output converter for 'kafka'?}
    F -->|Yes| G[toSend = convert]
    F -->|No| H[toSend = internal]
    G --> I[OutboundTransport.send]
    H --> I
    I --> J[Journal updateResponse]
```

### 4.2 Where converters are defined and used

| Layer | What | Where |
|-------|------|--------|
| **Registry** | Holds input/output converters per transport | **MessageConversionRegistry** (connector-transformation). |
| **Invocation** | Pipeline calls registry and runs converters | **ConnectorPipeline.process()** (connector-transformation): `getInputConverter(transport)`, `getOutputConverter(outputTransport)`. |
| **Demo implementation** | JMS → internal, internal → Kafka | **JmsToKafkaTransformation** (connector-demo-jms-kafka): registers **InputConverter&lt;ConnectorMessage&gt;** for `"jms"` and **OutputConverter&lt;ConnectorMessage&gt;** for `"kafka"` on the shared **MessageConversionRegistry** bean. |

### 4.3 Demo transformation behaviour

- **Input (JMS):** Decode payload as UTF-8, trim. Result is the “internal” **ConnectorMessage** (same structure, normalized payload).
- **Output (Kafka):** Take internal message, prefix payload with `"[JMS→Kafka] "`, build a new **ConnectorMessage** with `transportType = "kafka"`. That message is what gets sent to Kafka.

So: **transformation** = input converter (JMS → internal) + output converter (internal → Kafka format), both applied inside the pipeline and registered in **JmsToKafkaTransformation**.

---

## 5. Component diagram

```mermaid
flowchart TB
    subgraph Inbound["Inbound (JMS)"]
        Q[JMS Queue: connector-in]
        MLC[DefaultMessageListenerContainer]
        JIT[JmsInboundTransport]
        Q --> MLC
        MLC --> JIT
    end

    subgraph Bridge["Linking bridge"]
        MH[MessageHandler: jmsToPipelineHandler]
        CP[ConnectorPipeline]
        JIT --> MH
        MH --> CP
    end

    subgraph Transformation["Transformation"]
        MCR[MessageConversionRegistry]
        IN[JMS InputConverter]
        OUT[Kafka OutputConverter]
        MCR --> IN
        MCR --> OUT
        CP --> MCR
    end

    subgraph Persistence["Persistence & NFRs"]
        JW[JournalWriter]
        MET[ConnectorMetricsRegistry]
        CP --> JW
        CP --> MET
    end

    subgraph Outbound["Outbound (Kafka)"]
        OT[OutboundTransport]
        KT[KafkaTemplate]
        T[Kafka Topic: connector-out]
        CP --> OT
        OT --> KT
        KT --> T
    end

    IN --> CP
    OUT --> CP
```

---

## 6. Summary table

| Question | Answer |
|----------|--------|
| **Where is the transformation (input → output format)?** | In **ConnectorPipeline.process()**: it uses **MessageConversionRegistry** to run an optional **input** converter (JMS → internal) and an optional **output** converter (internal → Kafka). Demo converters are registered in **JmsToKafkaTransformation**. |
| **Where is the linking bridge (JMS consumed → Kafka published)?** | **DefaultMessageListenerContainer** (listens to JMS) → **JmsInboundTransport.onMessage()** (builds ConnectorMessage) → **MessageHandler** (jmsToPipelineHandler) → **ConnectorPipeline.process()** → **OutboundTransport.send()** (Kafka). Wiring is in **DemoJmsKafkaConfiguration** (container, handler, and `WireJmsHandler`). |
| **How to see transformation in the demo?** | Send a JMS message (e.g. via `POST /demo/send` with body `hello`). Consume from Kafka topic `connector-out`: the value should be **`[JMS→Kafka] hello`** (output converter prefix). |

---

## 7. References

- **Pipeline:** `connector-transformation/ConnectorPipeline.java`
- **Registry:** `connector-transformation/MessageConversionRegistry.java`
- **Demo config (bridge + pipeline):** `connector-demo-jms-kafka/DemoJmsKafkaConfiguration.java`
- **Demo transformation (converters):** `connector-demo-jms-kafka/JmsToKafkaTransformation.java`
- **Demo README:** `connector-demo-jms-kafka/README.md`
