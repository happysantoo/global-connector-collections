package com.example.connector.observability;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of per-transport metrics. Thread-safe.
 */
public final class ConnectorMetricsRegistry {

    private final Map<String, ConnectorMetrics> byTransport = new ConcurrentHashMap<>();

    public ConnectorMetrics getMetrics(String transport) {
        return byTransport.computeIfAbsent(transport, ConnectorMetrics::new);
    }
}
