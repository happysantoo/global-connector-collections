package com.example.connector.demo;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Helper endpoint to send a message to the JMS queue so the pipeline processes it.
 * For demo/testing only; in production, messages would come from external JMS producers.
 */
@RestController
@RequestMapping("/demo")
public class DemoSendController {

    private final jakarta.jms.ConnectionFactory connectionFactory;
    private final Queue connectorInputQueue;

    public DemoSendController(jakarta.jms.ConnectionFactory connectionFactory, Queue connectorInputQueue) {
        this.connectionFactory = connectionFactory;
        this.connectorInputQueue = connectorInputQueue;
    }

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody(required = false) byte[] body) {
        byte[] payload = body != null ? body : new byte[0];
        try (JMSContext ctx = connectionFactory.createContext()) {
            BytesMessage msg = ctx.createBytesMessage();
            msg.writeBytes(payload);
            ctx.createProducer().send(connectorInputQueue, msg);
            return ResponseEntity.accepted().body("Sent " + payload.length + " bytes to JMS queue connector-in");
        } catch (JMSException e) {
            return ResponseEntity.status(500).body("JMS error: " + e.getMessage());
        }
    }
}
