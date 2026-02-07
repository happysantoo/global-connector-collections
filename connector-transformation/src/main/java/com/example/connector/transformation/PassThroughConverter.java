package com.example.connector.transformation;

import com.example.connector.core.model.ConnectorMessage;
import com.example.connector.transformation.convert.InputConverter;
import com.example.connector.transformation.convert.OutputConverter;

/**
 * Example pass-through: input and output are ConnectorMessage (identity).
 */
public final class PassThroughConverter {

    public static final InputConverter<ConnectorMessage> INPUT = msg -> msg;
    public static final OutputConverter<ConnectorMessage> OUTPUT = msg -> msg;
}
