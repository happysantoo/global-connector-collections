package com.example.connector.core.journal;

import com.example.connector.core.model.ConnectorMessage;

import java.util.Optional;

/**
 * No-op implementation of JournalWriter. Does not persist anything.
 */
public final class NoOpJournalWriter implements JournalWriter {

    @Override
    public Optional<Long> appendRequest(ConnectorMessage message) {
        return Optional.empty();
    }

    @Override
    public void updateResponse(String correlationId, String status, byte[] responsePayload, String errorMessage) {
        // no-op
    }
}
