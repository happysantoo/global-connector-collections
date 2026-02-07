package com.example.connector.spring;

import com.example.connector.core.journal.JournalWriter;
import com.example.connector.journal.JdbcJournalWriter;
import com.example.connector.observability.ConnectorMetricsRegistry;
import com.example.connector.observability.ConnectorTracing;
import com.example.connector.transformation.MessageConversionRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Spring configuration for core pipeline, journal (JdbcTemplate + DataSource), transformation registry, observability.
 */
@Configuration
public class ConnectorSpringConfiguration {

    @Bean
    public MessageConversionRegistry messageConversionRegistry() {
        return new MessageConversionRegistry();
    }

    @Bean
    public ConnectorMetricsRegistry connectorMetricsRegistry() {
        return new ConnectorMetricsRegistry();
    }

    @Bean
    @ConditionalOnClass(name = "io.opentelemetry.api.trace.Tracer")
    @ConditionalOnBean(Tracer.class)
    public ConnectorTracing connectorTracing(Tracer tracer) {
        return new ConnectorTracing(tracer);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public JdbcJournalWriter jdbcJournalWriter(DataSource dataSource) {
        return new JdbcJournalWriter(new JdbcTemplate(dataSource));
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public JournalWriter journalWriter(JdbcJournalWriter jdbcJournalWriter) {
        return jdbcJournalWriter;
    }
}
