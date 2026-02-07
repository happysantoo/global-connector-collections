package com.example.connector.server.http;

import com.example.connector.core.transport.TransportRegistration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
public class ConnectorServerHttpAutoConfiguration {

    @Bean
    public HttpInboundTransport httpInboundTransport() {
        return new HttpInboundTransport();
    }

    @Bean
    public ConnectorHttpController connectorHttpController(HttpInboundTransport transport) {
        return new ConnectorHttpController(transport);
    }

    @Bean
    public TransportRegistration httpTransportRegistration(HttpInboundTransport transport) {
        return new TransportRegistration("http", transport);
    }

    @Bean
    public HealthIndicator connectorServerHttpHealthIndicator(HttpInboundTransport transport) {
        return () -> transport.isRunning()
                ? Health.up().withDetail("transport", "http").build()
                : Health.down().withDetail("transport", "http").build();
    }
}
