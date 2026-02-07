package com.example.connector.server.kafka;

import com.example.connector.core.transport.TransportRegistration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.springframework.kafka.core.ConsumerFactory")
@ConditionalOnProperty(name = "connector.servers.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaServerAutoConfiguration {

    @Bean
    public KafkaInboundTransport kafkaInboundTransport() {
        return new KafkaInboundTransport();
    }

    @Bean
    public TransportRegistration kafkaTransportRegistration(KafkaInboundTransport transport) {
        return new TransportRegistration("kafka", transport);
    }

    @Bean
    public HealthIndicator connectorServerKafkaHealthIndicator(KafkaInboundTransport transport) {
        return () -> transport.isRunning()
                ? Health.up().withDetail("transport", "kafka").build()
                : Health.down().withDetail("transport", "kafka").build();
    }
}
