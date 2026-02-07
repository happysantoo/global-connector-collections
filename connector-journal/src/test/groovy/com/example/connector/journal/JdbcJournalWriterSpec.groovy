package com.example.connector.journal

import com.example.connector.core.model.ConnectorMessage
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification

import java.time.Instant

class JdbcJournalWriterSpec extends Specification {

    def dataSource = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb")
            .addScript("schema.sql")
            .build()
    def jdbcTemplate = new JdbcTemplate(dataSource)
    def writer = new JdbcJournalWriter(jdbcTemplate)

    def cleanup() {
        dataSource?.shutdown()
    }

    def "should append request and get by correlation id"() {
        given:
        def message = new ConnectorMessage("corr-1", "http", "hello".bytes, Map.of(), Instant.now())

        when:
        def id = writer.appendRequest(message)

        then:
        id.isPresent()
        id.get() >= 1

        when:
        def entry = writer.getByCorrelationId("corr-1")

        then:
        entry.isPresent()
        entry.get().correlationId() == "corr-1"
        entry.get().status() == "RECEIVED"
        entry.get().direction() == "request"
    }

    def "should update response"() {
        given:
        def message = new ConnectorMessage("corr-2", "kafka", "req".bytes, Map.of(), Instant.now())
        writer.appendRequest(message)

        when:
        writer.updateResponse("corr-2", "SENT", "ok".bytes, null)

        then:
        def count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM connector_journal WHERE correlation_id = ? AND direction = 'response'",
                Integer.class, "corr-2")
        count == 1
    }
}
