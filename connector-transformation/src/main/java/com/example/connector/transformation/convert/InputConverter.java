package com.example.connector.transformation.convert;

import com.example.connector.core.model.ConnectorMessage;

/**
 * Converts from transport-specific input type to ConnectorMessage.
 *
 * @param <I> input type (e.g. HttpRequest, ConsumerRecord)
 */
@FunctionalInterface
public interface InputConverter<I> {

    ConnectorMessage convert(I input);
}
