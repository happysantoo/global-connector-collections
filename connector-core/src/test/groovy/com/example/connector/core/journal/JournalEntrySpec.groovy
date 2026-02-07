package com.example.connector.core.journal

import spock.lang.Specification

import java.time.Instant

class JournalEntrySpec extends Specification {

    def "toConnectorMessage should reconstruct message"() {
        given:
        def entry = new JournalEntry(
                1L, "corr-1", "request", "http",
                "application/json", "hello".bytes, "{}",
                "RECEIVED", Instant.now(), null, null
        )

        when:
        def msg = entry.toConnectorMessage()

        then:
        msg.correlationId() == "corr-1"
        msg.transportType() == "http"
        msg.payload() == "hello".bytes
    }
}
