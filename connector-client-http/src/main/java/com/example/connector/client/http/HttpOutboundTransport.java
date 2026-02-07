package com.example.connector.client.http;

import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;
import com.example.connector.core.correlation.CorrelationId;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP client that implements OutboundTransport. Sends ConnectorMessage to configurable URL.
 */
public final class HttpOutboundTransport implements OutboundTransport {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final Executor executor;

    public HttpOutboundTransport(String baseUrl, RestTemplate restTemplate, Executor executor) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<SendResult> send(ConnectorMessage message, Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = options != null && options.containsKey("url")
                        ? options.get("url").toString()
                        : baseUrl;
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.set(CorrelationId.getHeaderName(), message.correlationId());
                HttpEntity<byte[]> entity = new HttpEntity<>(message.payload(), headers);
                ResponseEntity<Void> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Void.class
                );
                if (response.getStatusCode().is2xxSuccessful()) {
                    return new SendResult.Success(response.getHeaders().getFirst("X-Request-Id") != null
                            ? response.getHeaders().getFirst("X-Request-Id")
                            : message.correlationId());
                }
                return new SendResult.Failure(new RuntimeException("HTTP " + response.getStatusCode()));
            } catch (Exception e) {
                return new SendResult.Failure(e);
            }
        }, executor);
    }
}
