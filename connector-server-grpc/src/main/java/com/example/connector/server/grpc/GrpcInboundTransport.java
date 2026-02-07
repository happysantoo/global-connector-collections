package com.example.connector.server.grpc;

import com.example.connector.core.correlation.CorrelationId;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.InboundTransport;
import com.example.connector.core.transport.MessageHandler;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * gRPC inbound transport: builds ConnectorMessage from request (correlation ID from metadata);
 * controllable start/stop. Wire to your gRPC service implementation.
 */
public final class GrpcInboundTransport implements InboundTransport {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile MessageHandler messageHandler;

    /**
     * Invoked by gRPC service layer when a request is received.
     */
    public void onRequest(String correlationId, byte[] payload, Map<String, String> metadata) {
        if (!running.get()) return;
        String cid = (correlationId != null && !correlationId.isBlank()) ? correlationId : CorrelationId.generate();
        ConnectorMessage message = new ConnectorMessage(
                cid,
                "grpc",
                payload != null ? payload : new byte[0],
                metadata != null ? metadata : Map.of(),
                Instant.now()
        );
        if (messageHandler != null) {
            messageHandler.handle(message);
        }
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
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
