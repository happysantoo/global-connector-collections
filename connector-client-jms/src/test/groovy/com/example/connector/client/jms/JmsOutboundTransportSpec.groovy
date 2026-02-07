package com.example.connector.client.jms

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.Executors

class JmsOutboundTransportSpec extends Specification {

    def "send returns CompletableFuture"() {
        given:
        def transport = new JmsOutboundTransport(null, "queue/out", false, Executors.newSingleThreadExecutor())
        def message = new ConnectorMessage("c1", "jms", "data".bytes, Map.of(), Instant.now())

        when:
        def future = transport.send(message, Map.of())

        then:
        future != null
    }
}
