package com.example.connector.server.jms

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import jakarta.jms.BytesMessage
import jakarta.jms.Session

class JmsInboundTransportSpec extends Specification {

    def transport = new JmsInboundTransport()

    def "should be running by default"() {
        expect:
        transport.isRunning()
    }

    def "stop then start"() {
        transport.stop()
        expect: !transport.isRunning()
        when: transport.start()
        then: transport.isRunning()
    }
}
