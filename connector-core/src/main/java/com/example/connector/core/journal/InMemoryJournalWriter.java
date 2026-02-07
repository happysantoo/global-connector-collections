package com.example.connector.core.journal;

import com.example.connector.core.model.ConnectorMessage;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of JournalWriter for tests. No persistence.
 */
public final class InMemoryJournalWriter implements JournalWriter {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<String, JournalEntry> byCorrelationId = new ConcurrentHashMap<>();

    @Override
    public Optional<Long> appendRequest(ConnectorMessage message) {
        long id = idGenerator.getAndIncrement();
        JournalEntry entry = new JournalEntry(
                id,
                message.correlationId(),
                "request",
                message.transportType(),
                "application/octet-stream",
                message.payload(),
                "{}",
                "RECEIVED",
                message.timestamp(),
                null,
                null
        );
        byCorrelationId.put(message.correlationId(), entry);
        return Optional.of(id);
    }

    @Override
    public void updateResponse(String correlationId, String status, byte[] responsePayload, String errorMessage) {
        JournalEntry request = byCorrelationId.get(correlationId);
        if (request != null) {
            JournalEntry updated = new JournalEntry(
                    request.id(),
                    request.correlationId(),
                    "response",
                    request.transport(),
                    "application/octet-stream",
                    responsePayload != null ? responsePayload : new byte[0],
                    "{}",
                    status,
                    request.createdAt(),
                    Instant.now(),
                    errorMessage
            );
            byCorrelationId.put(correlationId + ":response", updated);
        }
    }

    public Optional<JournalEntry> getByCorrelationId(String correlationId) {
        return Optional.ofNullable(byCorrelationId.get(correlationId));
    }
}
