package com.example.connector.client.grpc

import com.example.connector.core.model.ConnectorMessage
import com.example.connector.core.transport.SendResult
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.Executors

class GrpcOutboundTransportSpec extends Specification {

    def executor = Executors.newSingleThreadExecutor()
    def transport = new GrpcOutboundTransport("localhost:9090", executor)

    def "send returns success with correlation id"() {
        given:
        def message = new ConnectorMessage("c1", "grpc", "data".bytes, Map.of(), Instant.now())

        when:
        def result = transport.send(message, Map.of()).get()

        then:
        result instanceof SendResult.Success
        (result as SendResult.Success).messageId() == "c1"
    }
}
