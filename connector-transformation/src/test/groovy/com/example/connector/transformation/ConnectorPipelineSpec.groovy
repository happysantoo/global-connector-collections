package com.example.connector.transformation

import com.example.connector.core.journal.InMemoryJournalWriter
import com.example.connector.core.journal.JournalWriter
import com.example.connector.core.model.ConnectorMessage
import com.example.connector.core.transport.OutboundTransport
import com.example.connector.core.transport.SendResult
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.CompletableFuture

class ConnectorPipelineSpec extends Specification {

    def journalWriter = new InMemoryJournalWriter()
    def pipeline = new ConnectorPipeline(
            new MessageConversionRegistry(),
            journalWriter,
            { msg, opts -> CompletableFuture.completedFuture(new SendResult.Success("id-1")) } as OutboundTransport,
            "http"
    )

    def "should journal request and response on success"() {
        given:
        def message = new ConnectorMessage("corr-1", "http", "data".bytes, Map.of(), Instant.now())

        when:
        def result = pipeline.process(message, Map.of()).get()

        then:
        result instanceof SendResult.Success
        journalWriter.getByCorrelationId("corr-1").isPresent()
    }

    def "should journal FAILED when send fails"() {
        given:
        def failingTransport = { ConnectorMessage msg, Map opts ->
            CompletableFuture.failedFuture(new RuntimeException("send failed"))
        } as OutboundTransport
        def pipeline2 = new ConnectorPipeline(new MessageConversionRegistry(), journalWriter, failingTransport, "http")
        def message = new ConnectorMessage("corr-2", "http", new byte[0], Map.of(), Instant.now())

        when:
        pipeline2.process(message, Map.of()).exceptionally { it }.get()

        then:
        thrown(Exception)
        journalWriter.getByCorrelationId("corr-2:response").isPresent()
        journalWriter.getByCorrelationId("corr-2:response").get().status() == "FAILED"
    }
}
