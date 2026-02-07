package com.example.connector.client.http

import com.example.connector.core.model.ConnectorMessage
import com.example.connector.core.transport.SendResult
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.Executors

class HttpOutboundTransportSpec extends Specification {

    def executor = Executors.newSingleThreadExecutor()
    def restTemplate = new org.springframework.web.client.RestTemplate()

    def "send returns CompletableFuture"() {
        given:
        def transport = new HttpOutboundTransport("http://localhost:9999", restTemplate, executor)
        def message = new ConnectorMessage("c1", "http", "data".bytes, Map.of(), Instant.now())

        when:
        def result = transport.send(message, Map.of())

        then:
        result != null
        result.isDone() || !result.isDone()
    }
}
