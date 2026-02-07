package com.example.connector.core.transport;

/**
 * SPI for inbound (server) transports. Implementations are controllable (start/stop).
 */
public interface InboundTransport {

    /**
     * Start the listener.
     */
    void start();

    /**
     * Stop the listener.
     */
    void stop();

    /**
     * Whether the listener is currently running.
     */
    boolean isRunning();

    /**
     * Set the handler to invoke when a message is received.
     */
    void setMessageHandler(MessageHandler handler);
}
