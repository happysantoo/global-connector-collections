package com.example.connector.server.http

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import java.time.Instant

class HttpInboundTransportSpec extends Specification {

    def transport = new HttpInboundTransport()

    def "should be running by default"() {
        expect:
        transport.isRunning()
    }

    def "should accept messages when running"() {
        given:
        def received = []
        transport.setMessageHandler({ ConnectorMessage msg -> received << msg })
        def headers = ["X-Correlation-ID": "test-1"]

        when:
        def response = transport.receive("hello".bytes, headers)

        then:
        response.statusCode.value() == 202
        received.size() == 1
        received[0].correlationId() == "test-1"
    }

    def "stop then start should allow receiving again"() {
        given:
        transport.stop()

        when:
        def response = transport.receive(new byte[0], Map.of())

        then:
        response.statusCode.value() == 503

        when:
        transport.start()
        def response2 = transport.receive(new byte[0], Map.of())

        then:
        response2.statusCode.value() == 202
    }
}
