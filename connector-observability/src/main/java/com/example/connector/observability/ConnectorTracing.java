package com.example.connector.observability;

import com.example.connector.core.model.ConnectorMessage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Tracing helper: one span per request with correlation_id attribute; child spans for transform/send.
 * Returns AutoCloseable so callers (e.g. connector-transformation) need not depend on OTel context.
 */
public final class ConnectorTracing {

    private final Tracer tracer;

    public ConnectorTracing(Tracer tracer) {
        this.tracer = tracer;
    }

    /** Returns an AutoCloseable that must be closed when the span is done (e.g. in whenComplete). */
    public AutoCloseable startSpan(ConnectorMessage message, String spanName) {
        Span span = tracer.spanBuilder(spanName)
                .setAttribute("connector.correlation_id", message.correlationId())
                .setAttribute("connector.transport", message.transportType())
                .startSpan();
        Scope scope = span.makeCurrent();
        return scope::close;
    }
}
