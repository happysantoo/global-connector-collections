package com.example.connector.core.transport

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import java.time.Instant

class InboundTransportSpec extends Specification {

    def "message handler should be invokable"() {
        given:
        def received = []
        MessageHandler handler = { ConnectorMessage msg -> received.add(msg) }
        def message = new ConnectorMessage("c1", "http", "data".bytes, Map.of(), Instant.now())

        when:
        handler.handle(message)

        then:
        received.size() == 1
        received[0].correlationId() == "c1"
    }

    def "SendResult Success should hold messageId"() {
        when:
        def result = new SendResult.Success("msg-123")

        then:
        result.messageId() == "msg-123"
    }

    def "SendResult Failure should hold cause"() {
        given:
        def cause = new RuntimeException("send failed")

        when:
        def result = new SendResult.Failure(cause)

        then:
        result.cause() == cause
    }
}
