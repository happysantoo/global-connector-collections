package com.example.connector.core.transport;

/**
 * Result of an outbound send operation.
 */
public sealed interface SendResult permits SendResult.Success, SendResult.Failure {

    record Success(String messageId) implements SendResult {
    }

    record Failure(Throwable cause) implements SendResult {
    }
}
