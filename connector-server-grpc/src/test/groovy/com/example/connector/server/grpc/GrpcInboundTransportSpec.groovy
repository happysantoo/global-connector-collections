package com.example.connector.server.grpc

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

class GrpcInboundTransportSpec extends Specification {

    def transport = new GrpcInboundTransport()

    def "should be running by default"() {
        expect:
        transport.isRunning()
    }

    def "should delegate to handler when onRequest called"() {
        given:
        def received = []
        transport.setMessageHandler({ ConnectorMessage msg -> received << msg })
        def correlationId = "grpc-corr-1"
        def payload = "hello".bytes
        def metadata = ["key": "value"]

        when:
        transport.onRequest(correlationId, payload, metadata)

        then:
        received.size() == 1
        received[0].correlationId() == correlationId
        received[0].transportType() == "grpc"
        received[0].payload() == payload
    }

    def "stop then start"() {
        transport.stop()
        expect: !transport.isRunning()
        when: transport.start()
        then: transport.isRunning()
    }
}
