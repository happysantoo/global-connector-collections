package com.example.connector.core.correlation;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility to generate or extract correlation ID (e.g. from headers).
 */
public final class CorrelationId {

    private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    private CorrelationId() {
    }

    /**
     * Generate a new unique correlation ID.
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * Extract correlation ID from headers, or generate one if absent.
     */
    public static String fromHeadersOrGenerate(java.util.Map<String, String> headers) {
        return Optional.ofNullable(headers)
                .flatMap(h -> Optional.ofNullable(h.get(HEADER_CORRELATION_ID)))
                .filter(id -> !id.isBlank())
                .orElseGet(CorrelationId::generate);
    }

    public static String getHeaderName() {
        return HEADER_CORRELATION_ID;
    }
}
