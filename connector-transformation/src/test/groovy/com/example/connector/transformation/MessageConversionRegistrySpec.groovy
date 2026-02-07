package com.example.connector.transformation

import com.example.connector.core.model.ConnectorMessage
import com.example.connector.transformation.convert.InputConverter
import com.example.connector.transformation.convert.OutputConverter
import spock.lang.Specification

import java.time.Instant

class MessageConversionRegistrySpec extends Specification {

    def registry = new MessageConversionRegistry()

    def "should register and get input converter by transport"() {
        given:
        InputConverter<ConnectorMessage> conv = { msg -> msg }
        registry.registerInput("http", conv)

        when:
        def found = registry.getInputConverter("http", null)

        then:
        found.isPresent()
        found.get().convert(new ConnectorMessage("c1", "http", new byte[0], Map.of(), Instant.now())).correlationId() == "c1"
    }

    def "should register and get output converter"() {
        given:
        OutputConverter<ConnectorMessage> conv = { msg -> msg }
        registry.registerOutput("kafka", conv)

        when:
        def found = registry.getOutputConverter("kafka")

        then:
        found.isPresent()
    }

    def "should return empty for unknown transport"() {
        expect:
        registry.getInputConverter("unknown", null).isEmpty()
        registry.getOutputConverter("unknown").isEmpty()
    }
}
