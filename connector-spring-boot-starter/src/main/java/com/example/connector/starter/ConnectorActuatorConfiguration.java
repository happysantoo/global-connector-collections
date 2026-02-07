package com.example.connector.starter;

import com.example.connector.core.transport.InboundTransport;
import com.example.connector.core.transport.TransportRegistration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates all connector transports for Actuator: control endpoint and composite health.
 */
@Configuration
@ConditionalOnClass(HealthIndicator.class)
public class ConnectorActuatorConfiguration {

    @Bean
    public Map<String, InboundTransport> connectorTransports(
            java.util.List<TransportRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return Map.of();
        }
        return registrations.stream()
                .collect(Collectors.toMap(TransportRegistration::name, TransportRegistration::transport, (a, b) -> a));
    }

    @Bean
    public ConnectorControlEndpoint connectorControlEndpoint(Map<String, InboundTransport> connectorTransports) {
        return new ConnectorControlEndpoint(connectorTransports);
    }

    @Bean
    public HealthIndicator connectorServersHealthIndicator(Map<String, InboundTransport> connectorTransports) {
        return () -> {
            Health.Builder builder = new Health.Builder();
            boolean allUp = true;
            for (Map.Entry<String, InboundTransport> e : connectorTransports.entrySet()) {
                boolean up = e.getValue().isRunning();
                builder.withDetail(e.getKey(), up ? "UP" : "DOWN");
                if (!up) allUp = false;
            }
            return allUp ? builder.up().build() : builder.down().build();
        };
    }
}
