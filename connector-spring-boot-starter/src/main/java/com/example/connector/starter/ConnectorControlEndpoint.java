package com.example.connector.starter;

import com.example.connector.core.transport.InboundTransport;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Actuator endpoint to list all registered connector transports and allow start/stop by name.
 */
@Endpoint(id = "connector")
public class ConnectorControlEndpoint {

    private final Map<String, InboundTransport> transports;

    public ConnectorControlEndpoint(Map<String, InboundTransport> transports) {
        this.transports = transports != null ? transports : Map.of();
    }

    @ReadOperation
    public Map<String, Object> state() {
        Map<String, Object> transportStates = new LinkedHashMap<>();
        for (Map.Entry<String, InboundTransport> e : transports.entrySet()) {
            transportStates.put(e.getKey(), Map.of("running", e.getValue().isRunning()));
        }
        return Map.of("transports", transportStates);
    }

    @WriteOperation
    public Map<String, Object> control(String transport, String action) {
        Optional.ofNullable(transports.get(transport != null ? transport : "")).ifPresent(t -> {
            if ("start".equals(action)) {
                t.start();
            } else if ("stop".equals(action)) {
                t.stop();
            }
        });
        return state();
    }
}
