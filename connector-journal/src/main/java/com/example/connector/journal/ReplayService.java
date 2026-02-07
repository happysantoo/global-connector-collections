package com.example.connector.journal;

import com.example.connector.core.journal.JournalEntry;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Replay by correlation_id: read request from journal, re-run through transformation and outbound send.
 */
public final class ReplayService {

    private final JdbcJournalWriter journalWriter;
    private final OutboundTransport outboundTransport;

    public ReplayService(JdbcJournalWriter journalWriter, OutboundTransport outboundTransport) {
        this.journalWriter = journalWriter;
        this.outboundTransport = outboundTransport;
    }

    public Optional<CompletableFuture<SendResult>> replay(String correlationId, Map<String, Object> sendOptions) {
        return journalWriter.getByCorrelationId(correlationId)
                .map(entry -> {
                    ConnectorMessage message = entry.toConnectorMessage();
                    return outboundTransport.send(message, sendOptions != null ? sendOptions : Map.of());
                });
    }
}
