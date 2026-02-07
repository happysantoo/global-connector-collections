package com.example.connector.core.journal;

import com.example.connector.core.model.ConnectorMessage;

import java.time.Instant;
import java.util.Map;

/**
 * Read model for a journal entry (for replay).
 */
public record JournalEntry(
        long id,
        String correlationId,
        String direction,
        String transport,
        String payloadType,
        byte[] payloadBlob,
        String headersJson,
        String status,
        Instant createdAt,
        Instant processedAt,
        String errorMessage
) {
    /**
     * Reconstruct a ConnectorMessage from this entry (for replay).
     */
    public ConnectorMessage toConnectorMessage() {
        return new ConnectorMessage(
                correlationId,
                transport,
                payloadBlob != null ? payloadBlob : new byte[0],
                Map.of(), // headers could be parsed from headersJson if needed
                createdAt != null ? createdAt : Instant.now()
        );
    }
}
