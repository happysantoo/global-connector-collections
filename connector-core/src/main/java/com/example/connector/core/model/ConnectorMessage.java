package com.example.connector.core.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Canonical internal message carried through the connector pipeline.
 * Immutable; correlation ID is set at ingress and carried through journal, transform, and egress.
 */
public record ConnectorMessage(
        String correlationId,
        String transportType,
        byte[] payload,
        Map<String, String> headers,
        Instant timestamp
) {
    public ConnectorMessage {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be null or blank");
        }
        if (transportType == null || transportType.isBlank()) {
            throw new IllegalArgumentException("transportType must not be null or blank");
        }
        payload = payload != null ? payload : new byte[0];
        headers = headers != null ? Collections.unmodifiableMap(headers) : Map.of();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public ConnectorMessage withCorrelationId(String newCorrelationId) {
        return new ConnectorMessage(newCorrelationId, transportType, payload, headers, timestamp);
    }

    public ConnectorMessage withHeaders(Map<String, String> newHeaders) {
        return new ConnectorMessage(correlationId, transportType, payload, newHeaders, timestamp);
    }
}
