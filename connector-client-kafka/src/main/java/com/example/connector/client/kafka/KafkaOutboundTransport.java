package com.example.connector.client.kafka;

import com.example.connector.core.correlation.CorrelationId;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka outbound transport: sends ConnectorMessage to a topic; correlation ID in record headers.
 */
public final class KafkaOutboundTransport implements OutboundTransport {

    private final String defaultTopic;
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaOutboundTransport(String defaultTopic, KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.defaultTopic = defaultTopic;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<SendResult> send(ConnectorMessage message, Map<String, Object> options) {
        String topic = options != null && options.containsKey("topic")
                ? options.get("topic").toString()
                : defaultTopic;
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, message.payload());
        record.headers().add(CorrelationId.getHeaderName(), message.correlationId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return kafkaTemplate.send(record)
                .thenApply(springResult -> (SendResult) new SendResult.Success(
                        springResult.getProducerRecord().topic() + "-" + springResult.getRecordMetadata().offset()))
                .exceptionally(ex -> new SendResult.Failure(ex.getCause() != null ? ex.getCause() : ex));
    }
}
