package com.example.connector.core.model

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class ConnectorMessageSpec extends Specification {

    def "should create message with required fields"() {
        given:
        def correlationId = "corr-1"
        def transportType = "http"
        def payload = "hello".bytes
        def headers = ["X-Foo": "bar"]
        def timestamp = Instant.now()

        when:
        def msg = new ConnectorMessage(correlationId, transportType, payload, headers, timestamp)

        then:
        msg.correlationId() == correlationId
        msg.transportType() == transportType
        msg.payload() == payload
        msg.headers() == ["X-Foo": "bar"]
        msg.timestamp() == timestamp
    }

    def "should use default payload and timestamp when null"() {
        when:
        def msg = new ConnectorMessage("c1", "kafka", null, null, null)

        then:
        msg.payload().length == 0
        msg.headers().isEmpty()
        msg.timestamp() != null
    }

    @Unroll
    def "should throw when correlationId is #value"() {
        when:
        new ConnectorMessage(value, "http", new byte[0], Map.of(), Instant.now())

        then:
        thrown(IllegalArgumentException)

        where:
        value << [null, "", "   "]
    }

    @Unroll
    def "should throw when transportType is #value"() {
        when:
        new ConnectorMessage("c1", value, new byte[0], Map.of(), Instant.now())

        then:
        thrown(IllegalArgumentException)

        where:
        value << [null, "", "   "]
    }

    def "should return new message with updated correlationId"() {
        given:
        def msg = new ConnectorMessage("old", "http", "data".bytes, Map.of(), Instant.now())

        when:
        def updated = msg.withCorrelationId("new")

        then:
        updated.correlationId() == "new"
        msg.correlationId() == "old"
    }

    def "should return new message with updated headers"() {
        given:
        def msg = new ConnectorMessage("c1", "http", new byte[0], Map.of("A", "1"), Instant.now())

        when:
        def updated = msg.withHeaders(Map.of("B", "2"))

        then:
        updated.headers() == ["B": "2"]
        msg.headers() == ["A": "1"]
    }
}
