package com.example.connector.core.transport;

import com.example.connector.core.model.ConnectorMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SPI for outbound (client) transports. Send returns a CompletableFuture for async and back pressure.
 */
public interface OutboundTransport {

    /**
     * Send the message. Returns a future that completes with the send result.
     *
     * @param message the message to send
     * @param options optional transport-specific options (e.g. topic, queue name)
     * @return future of the send result
     */
    CompletableFuture<SendResult> send(ConnectorMessage message, Map<String, Object> options);
}
