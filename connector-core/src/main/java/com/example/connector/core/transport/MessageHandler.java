package com.example.connector.core.transport;

import com.example.connector.core.model.ConnectorMessage;

/**
 * Handler invoked when an inbound transport receives a message.
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * Handle the received message.
     *
     * @param message the canonical connector message
     */
    void handle(ConnectorMessage message);
}
