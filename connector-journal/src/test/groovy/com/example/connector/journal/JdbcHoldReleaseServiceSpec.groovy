package com.example.connector.journal

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.core.JdbcTemplate
import spock.lang.Specification

import java.time.Instant

class JdbcHoldReleaseServiceSpec extends Specification {

    def dataSource = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("holdtest")
            .addScript("schema.sql")
            .build()
    def jdbcTemplate = new JdbcTemplate(dataSource)
    def service = new JdbcHoldReleaseService(jdbcTemplate)

    def cleanup() {
        dataSource?.shutdown()
    }

    def "hold then release"() {
        when:
        service.hold("corr-1", Instant.now().plusSeconds(60), "test")

        then:
        service.release("corr-1")
        !service.release("corr-1")
    }

    def "listDueForRelease returns entries with held_until in past"() {
        given:
        service.hold("past-1", Instant.now().minusSeconds(1), "reason")

        when:
        def due = service.listDueForRelease()

        then:
        due.contains("past-1")
    }
}
