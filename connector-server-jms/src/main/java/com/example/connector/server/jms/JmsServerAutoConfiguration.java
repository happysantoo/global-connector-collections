package com.example.connector.server.jms;

import com.example.connector.core.transport.TransportRegistration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.jms.ConnectionFactory;

@Configuration
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnProperty(name = "connector.servers.jms.enabled", havingValue = "true", matchIfMissing = true)
public class JmsServerAutoConfiguration {

    @Bean
    public JmsInboundTransport jmsInboundTransport() {
        return new JmsInboundTransport();
    }

    @Bean
    public TransportRegistration jmsTransportRegistration(JmsInboundTransport transport) {
        return new TransportRegistration("jms", transport);
    }

    @Bean
    public HealthIndicator connectorServerJmsHealthIndicator(JmsInboundTransport transport) {
        return () -> transport.isRunning()
                ? Health.up().withDetail("transport", "jms").build()
                : Health.down().withDetail("transport", "jms").build();
    }
}
