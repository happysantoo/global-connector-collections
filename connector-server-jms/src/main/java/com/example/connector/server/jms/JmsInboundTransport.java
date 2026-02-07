package com.example.connector.server.jms;

import com.example.connector.core.batch.BatchBuffer;
import com.example.connector.core.correlation.CorrelationId;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.InboundTransport;
import com.example.connector.core.transport.MessageHandler;

import jakarta.jms.BytesMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JMS inbound transport: builds ConnectorMessage from JMS Message; controllable start/stop.
 * When a BatchBuffer is set, messages are offered to the buffer and a drain loop processes batches (back pressure).
 */
public final class JmsInboundTransport implements InboundTransport, MessageListener {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile MessageHandler messageHandler;
    private final BatchBuffer batchBuffer;
    private final long offerTimeoutMs;
    private volatile Thread batchProcessorThread;

    public JmsInboundTransport() {
        this(null, 5000L);
    }

    /**
     * @param batchBuffer optional; if non-null, messages are batched and processed by a drain loop (back pressure when full).
     * @param offerTimeoutMs timeout when offering to the buffer; used only when batchBuffer is non-null.
     */
    public JmsInboundTransport(BatchBuffer batchBuffer, long offerTimeoutMs) {
        this.batchBuffer = batchBuffer;
        this.offerTimeoutMs = offerTimeoutMs > 0 ? offerTimeoutMs : 5000L;
    }

    @Override
    public void onMessage(Message message) {
        if (!running.get()) return;
        try {
            String correlationId = getCorrelationId(message);
            byte[] payload = getPayload(message);
            Map<String, String> headers = getHeaders(message);
            ConnectorMessage cm = new ConnectorMessage(
                    correlationId,
                    "jms",
                    payload,
                    headers,
                    Instant.now()
            );
            if (batchBuffer != null) {
                try {
                    batchBuffer.offer(cm, offerTimeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted offering to batch buffer", e);
                }
            } else if (messageHandler != null) {
                messageHandler.handle(cm);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getCorrelationId(Message m) throws Exception {
        String id = m.getJMSCorrelationID();
        if (id != null && !id.isBlank()) return id;
        Object prop = m.getObjectProperty(CorrelationId.getHeaderName());
        if (prop != null) return prop.toString();
        return CorrelationId.generate();
    }

    private static byte[] getPayload(Message m) throws Exception {
        if (m instanceof BytesMessage bm) {
            bm.reset();
            byte[] buf = new byte[(int) bm.getBodyLength()];
            bm.readBytes(buf);
            return buf;
        }
        return new byte[0];
    }

    private static Map<String, String> getHeaders(Message m) throws Exception {
        Map<String, String> out = new HashMap<>();
        // Optional: enumerate property names and add to map
        return out;
    }

    @Override
    public void start() {
        running.set(true);
        if (batchBuffer != null && batchProcessorThread == null) {
            batchProcessorThread = new Thread(this::drainLoop, "jms-connector-batch-processor");
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
