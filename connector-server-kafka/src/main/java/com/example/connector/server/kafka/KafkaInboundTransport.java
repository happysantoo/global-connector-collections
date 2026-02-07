package com.example.connector.server.kafka;

import com.example.connector.core.batch.BatchBuffer;
import com.example.connector.core.correlation.CorrelationId;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.InboundTransport;
import com.example.connector.core.transport.MessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka inbound transport: builds ConnectorMessage from ConsumerRecord, delegates to MessageHandler.
 * When a BatchBuffer is set, messages are offered to the buffer and a drain loop processes batches (back pressure).
 * Controllable via start/stop (listener container lifecycle).
 */
public final class KafkaInboundTransport implements InboundTransport, MessageListener<String, byte[]> {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile MessageHandler messageHandler;
    private final BatchBuffer batchBuffer;
    private final long offerTimeoutMs;
    private volatile Thread batchProcessorThread;

    public KafkaInboundTransport() {
        this(null, 5000L);
    }

    /**
     * @param batchBuffer optional; if non-null, messages are batched and processed by a drain loop (back pressure when full).
     * @param offerTimeoutMs timeout when offering to the buffer (back pressure); used only when batchBuffer is non-null.
     */
    public KafkaInboundTransport(BatchBuffer batchBuffer, long offerTimeoutMs) {
        this.batchBuffer = batchBuffer;
        this.offerTimeoutMs = offerTimeoutMs > 0 ? offerTimeoutMs : 5000L;
    }

    @Override
    public void onMessage(ConsumerRecord<String, byte[]> record) {
        if (!running.get()) {
            return;
        }
        String correlationId = correlationFromHeaders(record);
        Map<String, String> headers = new HashMap<>();
        record.headers().forEach(h -> headers.put(h.key(), new String(h.value(), StandardCharsets.UTF_8)));
        ConnectorMessage message = new ConnectorMessage(
                correlationId,
                "kafka",
                record.value() != null ? record.value() : new byte[0],
                headers,
                Instant.ofEpochMilli(record.timestamp())
        );
        if (batchBuffer != null) {
            try {
                batchBuffer.offer(message, offerTimeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted offering to batch buffer", e);
            }
        } else if (messageHandler != null) {
            messageHandler.handle(message);
        }
    }

    private static String correlationFromHeaders(ConsumerRecord<String, byte[]> record) {
        var header = record.headers().lastHeader(CorrelationId.getHeaderName());
        if (header != null && header.value() != null) {
            String id = new String(header.value(), StandardCharsets.UTF_8);
            if (!id.isBlank()) return id;
        }
        return CorrelationId.generate();
    }

    @Override
    public void start() {
        running.set(true);
        if (batchBuffer != null && batchProcessorThread == null) {
            batchProcessorThread = new Thread(this::drainLoop, "kafka-connector-batch-processor");
            batchProcessorThread.setDaemon(true);
            batchProcessorThread.start();
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (batchProcessorThread != null) {
            batchProcessorThread.interrupt();
            batchProcessorThread = null;
        }
    }

    private void drainLoop() {
        while (running.get() && batchBuffer != null) {
            try {
                List<ConnectorMessage> batch = batchBuffer.drain();
                MessageHandler handler = messageHandler;
                if (handler != null) {
                    for (ConnectorMessage msg : batch) {
                        handler.handle(msg);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageHandler = handler;
    }
}
