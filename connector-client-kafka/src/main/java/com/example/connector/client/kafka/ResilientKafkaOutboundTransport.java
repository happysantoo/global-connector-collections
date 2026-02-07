package com.example.connector.client.kafka;

import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Wraps KafkaOutboundTransport with Resilience4j: Retry, Bulkhead, RateLimiter.
 */
public final class ResilientKafkaOutboundTransport implements OutboundTransport {

    private final OutboundTransport delegate;
    private final Retry retry;
    private final Bulkhead bulkhead;
    private final RateLimiter rateLimiter;
    private final Executor executor;

    public ResilientKafkaOutboundTransport(OutboundTransport delegate,
                                           Retry retry,
                                           Bulkhead bulkhead,
                                           RateLimiter rateLimiter,
                                           Executor executor) {
        this.delegate = delegate;
        this.retry = retry;
        this.bulkhead = bulkhead;
        this.rateLimiter = rateLimiter;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<SendResult> send(ConnectorMessage message, Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            rateLimiter.acquirePermission();
            return bulkhead.executeSupplier(
                    () -> retry.executeSupplier(() -> delegate.send(message, options).join())
            );
        }, executor);
    }
}
