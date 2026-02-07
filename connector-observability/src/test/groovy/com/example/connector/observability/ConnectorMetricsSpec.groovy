package com.example.connector.observability

import spock.lang.Specification

class ConnectorMetricsSpec extends Specification {

    def "should record received sent and failed"() {
        given:
        def metrics = new ConnectorMetrics("http")

        when:
        metrics.recordReceived()
        metrics.recordReceived()
        metrics.recordSent()
        metrics.recordFailed()

        then:
        metrics.getReceivedCount() == 2
        metrics.getSentCount() == 1
        metrics.getFailedCount() == 1
        metrics.getTransport() == "http"
    }
}
