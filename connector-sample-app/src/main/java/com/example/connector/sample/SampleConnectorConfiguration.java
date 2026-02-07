package com.example.connector.sample;

import com.example.connector.core.journal.JournalWriter;
import com.example.connector.core.transport.MessageHandler;
import com.example.connector.observability.ConnectorMetricsRegistry;
import com.example.connector.observability.ConnectorTracing;
import com.example.connector.server.http.HttpInboundTransport;
import com.example.connector.transformation.ConnectorPipeline;
import com.example.connector.transformation.MessageConversionRegistry;
import com.example.connector.client.kafka.KafkaOutboundTransport;
import com.example.connector.journal.JdbcJournalWriter;
import com.example.connector.journal.ReplayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Wires HTTP server → pipeline (journal + Kafka client). MessageHandler receives from HTTP and processes via pipeline.
 */
@Configuration
public class SampleConnectorConfiguration {

    @Bean
    public ConnectorPipeline connectorPipeline(
            MessageConversionRegistry registry,
            JournalWriter journalWriter,
            KafkaOutboundTransport kafkaOutboundTransport,
            ConnectorMetricsRegistry metricsRegistry,
            @Autowired(required = false) ConnectorTracing tracing,
            @Value("${connector.sample.kafka.topic:connector-out}") String topic) {
        return new ConnectorPipeline(registry, journalWriter, kafkaOutboundTransport, "kafka", tracing, metricsRegistry);
    }

    @Bean
    public KafkaTemplate<String, byte[]> kafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        ProducerFactory<String, byte[]> pf = new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(pf);
    }

    @Bean
    public ReplayService replayService(
            JdbcJournalWriter journalWriter,
            KafkaOutboundTransport kafkaOutboundTransport) {
        return new ReplayService(journalWriter, kafkaOutboundTransport);
    }

    @Bean
    public KafkaOutboundTransport kafkaOutboundTransport(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            @Value("${connector.sample.kafka.topic:connector-out}") String topic) {
        return new KafkaOutboundTransport(topic, kafkaTemplate);
    }

    @Bean
    public MessageHandler httpToPipelineHandler(ConnectorPipeline pipeline) {
        return message -> pipeline.process(message, Map.of("topic", "connector-out"));
    }

    @Configuration
    public static class WireHttpTransport {
        WireHttpTransport(HttpInboundTransport httpTransport, MessageHandler httpToPipelineHandler) {
            httpTransport.setMessageHandler(httpToPipelineHandler);
        }
    }
}
