package com.example.connector.demo;

import com.example.connector.client.kafka.KafkaOutboundTransport;
import com.example.connector.client.kafka.ResilientKafkaOutboundTransport;
import com.example.connector.core.journal.JournalWriter;
import com.example.connector.core.transport.MessageHandler;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.journal.JdbcHoldReleaseService;
import com.example.connector.journal.JdbcJournalWriter;
import com.example.connector.journal.HoldReleaseService;
import com.example.connector.journal.ReplayService;
import com.example.connector.observability.ConnectorMetricsRegistry;
import com.example.connector.observability.ConnectorTracing;
import com.example.connector.server.jms.JmsInboundTransport;
import com.example.connector.transformation.ConnectorPipeline;
import com.example.connector.transformation.MessageConversionRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.kafka.core.KafkaTemplate;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Demo: JMS inbound → pipeline (journal, observability, transform) → Kafka outbound (with resilience).
 * Optionally uses micro-batching for JMS (BatchBuffer + drain loop).
 */
@Configuration
public class DemoJmsKafkaConfiguration {

    private static final String JMS_QUEUE_IN = "connector-in";
    private static final String KAFKA_TOPIC_OUT = "connector-out";

    // ---- JMS ----
    @Bean
    public Queue connectorInputQueue() {
        return new ActiveMQQueue(JMS_QUEUE_IN);
    }

    @Bean
    public DefaultMessageListenerContainer jmsListenerContainer(
            ConnectionFactory connectionFactory,
            Queue connectorInputQueue,
            JmsInboundTransport jmsInboundTransport) {
        DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setDestination(connectorInputQueue);
        container.setMessageListener(jmsInboundTransport);
        container.setSessionTransacted(false);
        return container;
    }

    // ---- Pipeline: journal, observability, outbound ----
    @Bean
    public ConnectorPipeline connectorPipeline(
            MessageConversionRegistry registry,
            JournalWriter journalWriter,
            OutboundTransport kafkaOutboundTransport,
            ConnectorMetricsRegistry metricsRegistry,
            @Autowired(required = false) ConnectorTracing tracing) {
        return new ConnectorPipeline(
                registry, journalWriter, kafkaOutboundTransport, "kafka",
                tracing, metricsRegistry);
    }

    @Bean
    public MessageHandler jmsToPipelineHandler(ConnectorPipeline pipeline) {
        return message -> pipeline.process(message, Map.of("topic", KAFKA_TOPIC_OUT));
    }

    @Configuration
    public static class WireJmsHandler {
        WireJmsHandler(JmsInboundTransport jmsInboundTransport, MessageHandler jmsToPipelineHandler) {
            jmsInboundTransport.setMessageHandler(jmsToPipelineHandler);
        }
    }

    // ---- Kafka producer ----
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

    // ---- Kafka outbound with Resilience4j (Retry, Bulkhead, RateLimiter) ----
    @Bean
    public OutboundTransport kafkaOutboundTransport(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            @Value("${connector.demo.kafka.topic:" + KAFKA_TOPIC_OUT + "}") String topic,
            @Value("${connector.demo.resilience.enabled:true}") boolean resilienceEnabled) {
        KafkaOutboundTransport raw = new KafkaOutboundTransport(topic, kafkaTemplate);
        if (!resilienceEnabled) {
            return raw;
        }
        Retry retry = Retry.of("kafka-out", RetryConfig.<com.example.connector.core.transport.SendResult>custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .build());
        Bulkhead bulkhead = Bulkhead.of("kafka-out", BulkheadConfig.custom()
                .maxConcurrentCalls(10)
                .build());
        RateLimiter rateLimiter = RateLimiter.of("kafka-out", RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .build());
        return new ResilientKafkaOutboundTransport(
                raw, retry, bulkhead, rateLimiter,
                Executors.newVirtualThreadPerTaskExecutor());
    }

    // ---- Hold/Release ----
    @Bean
    public HoldReleaseService holdReleaseService(JdbcTemplate jdbcTemplate) {
        return new JdbcHoldReleaseService(jdbcTemplate);
    }

    // ---- Replay ----
    @Bean
    public ReplayService replayService(
            JdbcJournalWriter journalWriter,
            OutboundTransport kafkaOutboundTransport) {
        return new ReplayService(journalWriter, kafkaOutboundTransport);
    }
}
