package com.example.connector.demo;

import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.transformation.MessageConversionRegistry;
import com.example.connector.transformation.convert.InputConverter;
import com.example.connector.transformation.convert.OutputConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * Registers the transformation (input/output conversion) for the JMS → Kafka flow.
 *
 * <ul>
 *   <li><b>Input (JMS):</b> Normalize incoming JMS payload (trim, decode as UTF-8 for internal use).</li>
 *   <li><b>Output (Kafka):</b> Transform payload for Kafka (add envelope prefix so downstream can identify source).</li>
 * </ul>
 *
 * The pipeline calls these converters in order: input convert → journal → output convert → send.
 */
@Configuration
public class JmsToKafkaTransformation {

    private static final String PREFIX = "[JMS→Kafka] ";

    @Bean
    public JmsToKafkaTransformationRegistrar jmsToKafkaTransformationRegistrar(MessageConversionRegistry registry) {
        // Input: JMS → internal (normalize payload)
        registry.registerInput("jms", (InputConverter<ConnectorMessage>) msg -> {
            byte[] raw = msg.payload();
            String decoded = new String(raw != null ? raw : new byte[0], StandardCharsets.UTF_8).trim();
            return new ConnectorMessage(
                    msg.correlationId(),
                    msg.transportType(),
                    decoded.getBytes(StandardCharsets.UTF_8),
                    msg.headers(),
                    msg.timestamp()
            );
        });
        // Output: internal → Kafka format (add envelope)
        registry.registerOutput("kafka", (OutputConverter<ConnectorMessage>) msg -> {
            byte[] payload = msg.payload();
            String body = payload != null && payload.length > 0
                    ? new String(payload, StandardCharsets.UTF_8)
                    : "";
            String out = PREFIX + body;
            return new ConnectorMessage(
                    msg.correlationId(),
                    "kafka",
                    out.getBytes(StandardCharsets.UTF_8),
                    msg.headers(),
                    msg.timestamp()
            );
        });
        return new JmsToKafkaTransformationRegistrar();
    }

    /** Marker bean so registration runs (registry is mutated in the bean above). */
    public static final class JmsToKafkaTransformationRegistrar {}
}
