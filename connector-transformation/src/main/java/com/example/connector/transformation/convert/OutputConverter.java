package com.example.connector.transformation.convert;

import com.example.connector.core.model.ConnectorMessage;

/**
 * Converts from ConnectorMessage to transport-specific output type.
 *
 * @param <O> output type (e.g. ProducerRecord, HttpEntity)
 */
@FunctionalInterface
public interface OutputConverter<O> {

    O convert(ConnectorMessage message);
}
