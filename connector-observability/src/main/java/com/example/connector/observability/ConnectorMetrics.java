package com.example.connector.observability;

import java.util.concurrent.atomic.LongAdder;

/**
 * Simple metrics: counters for received/sent/failed per transport; optional histograms.
 */
public final class ConnectorMetrics {

    private final LongAdder received = new LongAdder();
    private final LongAdder sent = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final String transport;

    public ConnectorMetrics(String transport) {
        this.transport = transport;
    }

    public void recordReceived() {
        received.increment();
    }

    public void recordSent() {
        sent.increment();
    }

    public void recordFailed() {
        failed.increment();
    }

    public long getReceivedCount() {
        return received.sum();
    }

    public long getSentCount() {
        return sent.sum();
    }

    public long getFailedCount() {
        return failed.sum();
    }

    public String getTransport() {
        return transport;
    }
}
