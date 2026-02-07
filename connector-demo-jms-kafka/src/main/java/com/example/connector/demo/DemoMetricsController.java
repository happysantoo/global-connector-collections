package com.example.connector.demo;

import com.example.connector.observability.ConnectorMetricsRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple endpoint to expose pipeline metrics (received/sent/failed per transport).
 * Demonstrates observability; for production use Actuator metrics or OpenTelemetry.
 */
@RestController
@RequestMapping("/connector/metrics")
public class DemoMetricsController {

    private final ConnectorMetricsRegistry metricsRegistry;

    public DemoMetricsController(ConnectorMetricsRegistry metricsRegistry) {
        this.metricsRegistry = metricsRegistry;
    }

    @GetMapping
    public Map<String, Object> metrics() {
        var jms = metricsRegistry.getMetrics("jms");
        return Map.of(
                "jms", Map.of(
                        "received", jms.getReceivedCount(),
                        "sent", jms.getSentCount(),
                        "failed", jms.getFailedCount()
                )
        );
    }
}
