package com.example.connector.core.journal

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import java.time.Instant

class NoOpJournalWriterSpec extends Specification {

    def writer = new NoOpJournalWriter()

    def "appendRequest should return empty"() {
        given:
        def message = new ConnectorMessage("c1", "http", new byte[0], Map.of(), Instant.now())

        when:
        def id = writer.appendRequest(message)

        then:
        id.isEmpty()
    }

    def "updateResponse should not throw"() {
        when:
        writer.updateResponse("c1", "SENT", null, null)

        then:
        noExceptionThrown()
    }
}
