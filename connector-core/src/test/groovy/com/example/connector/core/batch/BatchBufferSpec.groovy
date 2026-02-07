package com.example.connector.core.batch

import com.example.connector.core.model.ConnectorMessage
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

class BatchBufferSpec extends Specification {

    def "offer and drain"() {
        given:
        def buffer = new BatchBuffer(10, 3, Duration.ofMillis(50))
        def msg = new ConnectorMessage("c1", "kafka", new byte[0], Map.of(), Instant.now())

        when:
        buffer.offer(msg, 1000)
        def batch = buffer.drain()

        then:
        batch.size() == 1
        batch[0].correlationId() == "c1"
    }

    def "drain respects maxBatchSize"() {
        given:
        def buffer = new BatchBuffer(10, 2, Duration.ofMillis(10))
        def msg1 = new ConnectorMessage("c1", "kafka", new byte[0], Map.of(), Instant.now())
        def msg2 = new ConnectorMessage("c2", "kafka", new byte[0], Map.of(), Instant.now())
        def msg3 = new ConnectorMessage("c3", "kafka", new byte[0], Map.of(), Instant.now())
        buffer.offer(msg1, 1000)
        buffer.offer(msg2, 1000)
        buffer.offer(msg3, 1000)

        when:
        def batch = buffer.drain()

        then:
        batch.size() == 2
        buffer.size() == 1
    }
}
