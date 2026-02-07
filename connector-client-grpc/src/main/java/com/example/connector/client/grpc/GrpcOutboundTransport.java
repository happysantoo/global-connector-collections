package com.example.connector.client.grpc;

import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * gRPC outbound transport stub: sends ConnectorMessage to a target (correlation ID in metadata).
 * Replace with real gRPC stub when proto and channel are configured.
 */
public final class GrpcOutboundTransport implements OutboundTransport {

    private final String target;
    private final Executor executor;

    public GrpcOutboundTransport(String target, Executor executor) {
        this.target = target;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<SendResult> send(ConnectorMessage message, Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            // Stub: in real impl, call gRPC channel with message and metadata (correlation ID).
            return (SendResult) new SendResult.Success(message.correlationId());
        }, executor);
    }
}
