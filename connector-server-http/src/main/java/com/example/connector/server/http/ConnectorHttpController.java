package com.example.connector.server.http;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that delegates to HttpInboundTransport. Registered as bean by ConnectorServerHttpAutoConfiguration.
 */
@RestController
@RequestMapping("/connector/messages")
public class ConnectorHttpController {

    private final HttpInboundTransport transport;

    public ConnectorHttpController(HttpInboundTransport transport) {
        this.transport = transport;
    }

    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> postBinary(@RequestBody(required = false) byte[] body,
                                           @RequestHeader Map<String, String> headers) {
        return transport.receive(body, headers);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> postJson(@RequestBody(required = false) byte[] body,
                                         @RequestHeader Map<String, String> headers) {
        return transport.receive(body, headers);
    }
}
