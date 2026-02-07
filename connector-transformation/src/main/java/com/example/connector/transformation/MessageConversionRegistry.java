package com.example.connector.transformation;

import com.example.connector.transformation.convert.InputConverter;
import com.example.connector.transformation.convert.OutputConverter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for input/output converters by transport and optional content type.
 */
public final class MessageConversionRegistry {

    private final Map<String, InputConverter<?>> inputConverters = new ConcurrentHashMap<>();
    private final Map<String, OutputConverter<?>> outputConverters = new ConcurrentHashMap<>();

    /**
     * Register an input converter for the given transport (and optional content type key).
     * Key format: "transport" or "transport:contentType"
     */
    public <I> void registerInput(String transport, String contentType, InputConverter<I> converter) {
        String key = contentType != null && !contentType.isBlank() ? transport + ":" + contentType : transport;
        inputConverters.put(key, converter);
    }

    public <I> void registerInput(String transport, InputConverter<I> converter) {
        registerInput(transport, null, converter);
    }

    /**
     * Register an output converter for the given transport.
     */
    public <O> void registerOutput(String transport, OutputConverter<O> converter) {
        outputConverters.put(transport, converter);
    }

    @SuppressWarnings("unchecked")
    public <I> Optional<InputConverter<I>> getInputConverter(String transport, String contentType) {
        String key = contentType != null && !contentType.isBlank() ? transport + ":" + contentType : transport;
        return Optional.ofNullable((InputConverter<I>) inputConverters.get(key))
                .or(() -> Optional.ofNullable((InputConverter<I>) inputConverters.get(transport)));
    }

    @SuppressWarnings("unchecked")
    public <O> Optional<OutputConverter<O>> getOutputConverter(String transport) {
        return Optional.ofNullable((OutputConverter<O>) outputConverters.get(transport));
    }
}
