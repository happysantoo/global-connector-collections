package com.example.connector.core.journal;

import com.example.connector.core.model.ConnectorMessage;

import java.util.Optional;

/**
 * SPI for appending and updating journal entries (request/response).
 */
public interface JournalWriter {

    /**
     * Append a request entry with status RECEIVED.
     *
     * @param message the received message
     * @return the journal entry id, if supported
     */
    Optional<Long> appendRequest(ConnectorMessage message);

    /**
     * Update or append response for the given correlation ID.
     *
     * @param correlationId correlation ID of the request
     * @param status        SENT or FAILED
     * @param responsePayload optional response payload
     * @param errorMessage  optional error message when status is FAILED
     */
    void updateResponse(String correlationId, String status, byte[] responsePayload, String errorMessage);
}
