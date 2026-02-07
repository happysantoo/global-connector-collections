package com.example.connector.server.http;

import com.example.connector.core.correlation.CorrelationId;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.InboundTransport;
import com.example.connector.core.transport.MessageHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP server that implements InboundTransport. Controllable (start/stop).
 */
public final class HttpInboundTransport implements InboundTransport {

    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile MessageHandler messageHandler;

    /**
     * Invoked by REST controller: build ConnectorMessage and delegate to handler.
     */
    public ResponseEntity<Void> receive(byte[] body, Map<String, String> headers) {
        if (!running.get()) {
            return ResponseEntity.status(503).build();
        }
        String correlationId = CorrelationId.fromHeadersOrGenerate(headers != null ? headers : Map.of());
        ConnectorMessage message = new ConnectorMessage(
                correlationId,
                "http",
                body != null ? body : new byte[0],
                headers != null ? headers : Map.of(),
                Instant.now()
        );
        if (messageHandler != null) {
            messageHandler.handle(message);
        }
        return ResponseEntity.accepted().build();
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
