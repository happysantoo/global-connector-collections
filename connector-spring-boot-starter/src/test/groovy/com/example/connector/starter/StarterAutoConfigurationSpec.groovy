package com.example.connector.starter

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = EmptyBootApp)
@TestPropertySource(properties = ["management.endpoints.web.exposure.include=connector,health"])
class StarterAutoConfigurationSpec extends Specification {

    @Autowired(required = false)
    ConnectorControlEndpoint connectorControlEndpoint

    @Autowired(required = false)
    @Qualifier("connectorServersHealthIndicator")
    HealthIndicator connectorServersHealthIndicator

    def "context loads with starter"() {
        expect:
        true
    }

    def "connector control endpoint lists transports"() {
        when:
        def state = connectorControlEndpoint?.state()

        then:
        connectorControlEndpoint != null
        state != null
        state.transports != null
        state.transports.containsKey("http")
        state.transports.http.running == true
    }

    def "connector control endpoint supports start/stop"() {
        when:
        connectorControlEndpoint.control("http", "stop")
        def stopped = connectorControlEndpoint.state()
        connectorControlEndpoint.control("http", "start")
        def started = connectorControlEndpoint.state()

        then:
        stopped.transports.http.running == false
        started.transports.http.running == true
    }

    def "connectorServers health indicator is present"() {
        when:
        def health = connectorServersHealthIndicator?.health()

        then:
        connectorServersHealthIndicator != null
        health != null
        health.status != null
    }
}
