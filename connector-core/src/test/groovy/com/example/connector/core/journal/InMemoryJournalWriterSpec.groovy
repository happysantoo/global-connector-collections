package com.example.connector.core.journal

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import java.time.Instant

class InMemoryJournalWriterSpec extends Specification {

    def writer = new InMemoryJournalWriter()

    def "should append request and return id"() {
        given:
        def message = new ConnectorMessage("corr-1", "http", "payload".bytes, Map.of(), Instant.now())

        when:
        def id = writer.appendRequest(message)

        then:
        id.isPresent()
        id.get() >= 1
        writer.getByCorrelationId("corr-1").isPresent()
        writer.getByCorrelationId("corr-1").get().status() == "RECEIVED"
    }

    def "should update response for correlation id"() {
        given:
        def message = new ConnectorMessage("corr-2", "kafka", "req".bytes, Map.of(), Instant.now())
        writer.appendRequest(message)

        when:
        writer.updateResponse("corr-2", "SENT", "response".bytes, null)

        then:
        def responseEntry = writer.getByCorrelationId("corr-2:response")
        responseEntry.isPresent()
        responseEntry.get().status() == "SENT"
    }

    def "should update response with FAILED status and error message"() {
        given:
        def message = new ConnectorMessage("corr-3", "http", new byte[0], Map.of(), Instant.now())
        writer.appendRequest(message)

        when:
        writer.updateResponse("corr-3", "FAILED", null, "Connection refused")

        then:
        def responseEntry = writer.getByCorrelationId("corr-3:response")
        responseEntry.isPresent()
        responseEntry.get().status() == "FAILED"
        responseEntry.get().errorMessage() == "Connection refused"
    }
}
