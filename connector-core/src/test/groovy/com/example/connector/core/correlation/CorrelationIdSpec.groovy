package com.example.connector.core.correlation

import spock.lang.Specification

class CorrelationIdSpec extends Specification {

    def "should generate non-blank unique correlation id"() {
        when:
        def id1 = CorrelationId.generate()
        def id2 = CorrelationId.generate()

        then:
        id1 != null
        !id1.isBlank()
        id1 != id2
    }

    def "should extract correlation id from headers when present"() {
        given:
        def headers = ["X-Correlation-ID": "existing-id", "Other": "value"]

        when:
        def id = CorrelationId.fromHeadersOrGenerate(headers)

        then:
        id == "existing-id"
    }

    def "should generate new id when headers null"() {
        when:
        def id = CorrelationId.fromHeadersOrGenerate(null)

        then:
        id != null
        !id.isBlank()
    }

    def "should generate new id when header missing"() {
        when:
        def id = CorrelationId.fromHeadersOrGenerate(["Other": "value"])

        then:
        id != null
        !id.isBlank()
    }

    def "should generate new id when header blank"() {
        when:
        def id = CorrelationId.fromHeadersOrGenerate(["X-Correlation-ID": "  "])

        then:
        id != null
        !id.isBlank()
    }

    def "should return standard header name"() {
        expect:
        CorrelationId.getHeaderName() == "X-Correlation-ID"
    }
}
