package com.example.connector.server.kafka

import com.example.connector.core.model.ConnectorMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.record.TimestampType
import spock.lang.Specification

import java.util.Optional

class KafkaInboundTransportSpec extends Specification {

    def transport = new KafkaInboundTransport()

    def "should be running by default"() {
        expect:
        transport.isRunning()
    }

    def "should build message from record and delegate to handler"() {
        given:
        def received = []
        transport.setMessageHandler({ ConnectorMessage msg -> received << msg })
        def headers = new RecordHeaders()
        headers.add("X-Correlation-ID", "kafka-corr-1".bytes)
        def record = new ConsumerRecord<>("topic", 0, 0L, 0L, TimestampType.CREATE_TIME, 0, 0, "key", "hello".bytes, headers, Optional.empty())

        when:
        transport.onMessage(record)

        then:
        received.size() == 1
        received[0].correlationId() == "kafka-corr-1"
        received[0].transportType() == "kafka"
        received[0].payload() == "hello".bytes
    }

    def "stop then start"() {
        transport.stop()
        expect: !transport.isRunning()
        when: transport.start()
        then: transport.isRunning()
    }
}
