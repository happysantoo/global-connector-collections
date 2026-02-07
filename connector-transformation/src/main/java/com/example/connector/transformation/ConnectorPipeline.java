package com.example.connector.transformation;

import com.example.connector.core.journal.JournalWriter;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.MessageHandler;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;
import com.example.connector.observability.ConnectorMetricsRegistry;
import com.example.connector.observability.ConnectorTracing;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Pipeline: receive → input convert → journal request → transform (user) → output convert → send → journal response.
 * Optional tracing and metrics are applied when provided.
 */
public final class ConnectorPipeline {

    private final MessageConversionRegistry registry;
    private final JournalWriter journalWriter;
    private final OutboundTransport outboundTransport;
    private final String outputTransport;
    private final ConnectorTracing tracing;
    private final ConnectorMetricsRegistry metricsRegistry;

    public ConnectorPipeline(
            MessageConversionRegistry registry,
            JournalWriter journalWriter,
            OutboundTransport outboundTransport,
            String outputTransport) {
        this(registry, journalWriter, outboundTransport, outputTransport, null, null);
    }

    public ConnectorPipeline(
            MessageConversionRegistry registry,
            JournalWriter journalWriter,
            OutboundTransport outboundTransport,
            String outputTransport,
            ConnectorTracing tracing,
            ConnectorMetricsRegistry metricsRegistry) {
        this.registry = registry;
        this.journalWriter = journalWriter;
        this.outboundTransport = outboundTransport;
        this.outputTransport = outputTransport;
        this.tracing = tracing;
        this.metricsRegistry = metricsRegistry;
    }

    /**
     * Process an already-built ConnectorMessage (e.g. from an inbound transport).
     * Flow: optional input convert → journal request → optional output convert → send → journal response.
     * When observability is configured: one span per request (correlation_id attribute), metrics received/sent/failed.
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<SendResult> process(ConnectorMessage message, Map<String, Object> sendOptions) {
        if (metricsRegistry != null) {
            metricsRegistry.getMetrics(message.transportType()).recordReceived();
        }
        AutoCloseable scope = tracing != null ? tracing.startSpan(message, "connector.process") : null;
        // Optional input conversion (e.g. normalize JMS payload to internal format)
        var inputOpt = registry.getInputConverter(message.transportType(), null);
        ConnectorMessage internal = inputOpt.isPresent()
                ? (ConnectorMessage) ((com.example.connector.transformation.convert.InputConverter) inputOpt.get()).convert(message)
                : message;
        journalWriter.appendRequest(internal);
        // Optional output conversion (e.g. format payload for Kafka)
        var outputOpt = registry.getOutputConverter(outputTransport);
        ConnectorMessage toSend = outputOpt.isPresent()
                ? (ConnectorMessage) ((com.example.connector.transformation.convert.OutputConverter) outputOpt.get()).convert(internal)
                : internal;
        return outboundTransport.send(toSend, sendOptions != null ? sendOptions : Map.of())
                .whenComplete((result, ex) -> {
                    if (scope != null) {
                        try {
                            scope.close();
                        } catch (Exception ignored) {
                        }
                    }
                    if (metricsRegistry != null) {
                        var metrics = metricsRegistry.getMetrics(message.transportType());
                        if (ex != null) {
                            metrics.recordFailed();
                        } else if (result instanceof SendResult.Success) {
                            metrics.recordSent();
                        } else if (result instanceof SendResult.Failure) {
                            metrics.recordFailed();
                        }
                    }
                    if (ex != null) {
                        journalWriter.updateResponse(internal.correlationId(), "FAILED", null, ex.getMessage());
                    } else if (result instanceof SendResult.Success s) {
                        journalWriter.updateResponse(internal.correlationId(), "SENT", null, null);
                    } else if (result instanceof SendResult.Failure f) {
                        journalWriter.updateResponse(internal.correlationId(), "FAILED", null, f.cause().getMessage());
                    }
                });
    }

    public MessageConversionRegistry getRegistry() {
        return registry;
    }
}
