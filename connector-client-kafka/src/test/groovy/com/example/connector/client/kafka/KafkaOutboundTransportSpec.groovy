package com.example.connector.client.kafka

import com.example.connector.core.model.ConnectorMessage
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.CompletableFuture

class KafkaOutboundTransportSpec extends Specification {

    def "send returns CompletableFuture"() {
        given:
        def template = Mock(KafkaTemplate)
        template.send(_) >> CompletableFuture.completedFuture(new SendResult(null, null))
        def transport = new KafkaOutboundTransport("connector-out", template)
        def message = new ConnectorMessage("c1", "kafka", "data".bytes, Map.of(), Instant.now())

        when:
        def result = transport.send(message, Map.of())

        then:
        result != null
        1 * template.send(_)
    }
}
