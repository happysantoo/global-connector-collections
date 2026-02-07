package com.example.connector.journal

import com.example.connector.core.model.ConnectorMessage
import com.example.connector.core.transport.OutboundTransport
import com.example.connector.core.transport.SendResult
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

class ReplayServiceSpec extends Specification {

    def dataSource = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("replay-testdb")
            .addScript("schema.sql")
            .build()
    def jdbcTemplate = new JdbcTemplate(dataSource)
    def journalWriter = new JdbcJournalWriter(jdbcTemplate)
    def sentMessages = new ConcurrentLinkedQueue<ConnectorMessage>()
    def outboundTransport = { ConnectorMessage msg, Map opts ->
        sentMessages.add(msg)
        return CompletableFuture.completedFuture(new SendResult.Success("replayed"))
    } as OutboundTransport
    def replayService = new ReplayService(journalWriter, outboundTransport)

    def cleanup() {
        dataSource?.shutdown()
    }

    def "replay returns empty when correlation id not in journal"() {
        when:
        def result = replayService.replay("nonexistent", Map.of())

        then:
        result.isEmpty()
        sentMessages.isEmpty()
    }

    def "replay sends message via outbound transport when entry exists"() {
        given:
        def message = new ConnectorMessage("replay-1", "http", "payload".bytes, Map.of(), Instant.now())
        journalWriter.appendRequest(message)

        when:
        def opt = replayService.replay("replay-1", Map.of("topic", "out"))

        then:
        opt.isPresent()
        opt.get().get() instanceof SendResult.Success
        sentMessages.size() == 1
        sentMessages.peek().correlationId() == "replay-1"
        sentMessages.peek().transportType() == "http"
        new String(sentMessages.peek().payload()) == "payload"
    }

    def "replay passes send options to outbound transport"() {
        given:
        def message = new ConnectorMessage("replay-2", "kafka", "data".bytes, Map.of(), Instant.now())
        journalWriter.appendRequest(message)
        def options = Map.of("topic", "custom-topic")

        when:
        replayService.replay("replay-2", options).get().get()

        then:
        sentMessages.size() == 1
    }
}
