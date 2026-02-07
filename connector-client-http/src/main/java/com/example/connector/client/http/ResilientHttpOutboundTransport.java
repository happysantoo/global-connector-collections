package com.example.connector.client.http;

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
 * Wraps HttpOutboundTransport with Resilience4j: Retry, Bulkhead, optional RateLimiter.
 */
public final class ResilientHttpOutboundTransport implements OutboundTransport {

    private final OutboundTransport delegate;
    private final Retry retry;
    private final Bulkhead bulkhead;
    private final RateLimiter rateLimiter;
    private final Executor executor;

    public ResilientHttpOutboundTransport(OutboundTransport delegate,
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
