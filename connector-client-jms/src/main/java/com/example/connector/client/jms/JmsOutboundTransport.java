package com.example.connector.client.jms;

import com.example.connector.core.correlation.CorrelationId;
import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.core.transport.OutboundTransport;
import com.example.connector.core.transport.SendResult;

import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Queue;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * JMS outbound transport: sends ConnectorMessage to queue/topic; correlation ID in message property.
 */
public final class JmsOutboundTransport implements OutboundTransport {

    private final ConnectionFactory connectionFactory;
    private final String defaultDestinationName;
    private final boolean useTopic;
    private final Executor executor;

    public JmsOutboundTransport(ConnectionFactory connectionFactory, String defaultDestinationName, boolean useTopic, Executor executor) {
        this.connectionFactory = connectionFactory;
        this.defaultDestinationName = defaultDestinationName;
        this.useTopic = useTopic;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<SendResult> send(ConnectorMessage message, Map<String, Object> options) {
        return CompletableFuture.supplyAsync(() -> {
            try (JMSContext ctx = connectionFactory.createContext()) {
                String destName = options != null && options.containsKey("destination")
                        ? options.get("destination").toString()
                        : defaultDestinationName;
                var dest = useTopic ? ctx.createTopic(destName) : ctx.createQueue(destName);
                JMSProducer producer = ctx.createProducer();
                BytesMessage bm = ctx.createBytesMessage();
                bm.writeBytes(message.payload());
                bm.setJMSCorrelationID(message.correlationId());
                bm.setStringProperty(CorrelationId.getHeaderName(), message.correlationId());
                producer.send(dest, bm);
                return (SendResult) new SendResult.Success(message.correlationId());
            } catch (Exception e) {
                return new SendResult.Failure(e);
            }
        }, executor);
    }
}
