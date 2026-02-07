package com.example.connector.server.grpc;

import com.example.connector.core.transport.TransportRegistration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "io.grpc.BindableService")
@ConditionalOnProperty(name = "connector.servers.grpc.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcServerAutoConfiguration {

    @Bean
    public GrpcInboundTransport grpcInboundTransport() {
        return new GrpcInboundTransport();
    }

    @Bean
    public TransportRegistration grpcTransportRegistration(GrpcInboundTransport transport) {
        return new TransportRegistration("grpc", transport);
    }

    @Bean
    public HealthIndicator connectorServerGrpcHealthIndicator(GrpcInboundTransport transport) {
        return () -> transport.isRunning()
                ? Health.up().withDetail("transport", "grpc").build()
                : Health.down().withDetail("transport", "grpc").build();
    }
}
