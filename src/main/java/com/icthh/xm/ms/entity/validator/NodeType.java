package com.icthh.xm.ms.entity.validator;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

@AllArgsConstructor
public enum NodeType {
    ARRAY("array"),
    BOOLEAN("boolean"),
    INTEGER("integer"),
    NULL("null"),
    NUMBER("number"),
    OBJECT("object"),
    STRING("string");

    private final String name;

    private static final Map<String, NodeType> NAME_MAP;

    private static final Map<JsonToken, NodeType> TOKEN_MAP = new EnumMap<>(JsonToken.class);

    static {
        TOKEN_MAP.put(JsonToken.START_ARRAY, ARRAY);
        TOKEN_MAP.put(JsonToken.VALUE_TRUE, BOOLEAN);
        TOKEN_MAP.put(JsonToken.VALUE_FALSE, BOOLEAN);
        TOKEN_MAP.put(JsonToken.VALUE_NUMBER_INT, INTEGER);
        TOKEN_MAP.put(JsonToken.VALUE_NUMBER_FLOAT, NUMBER);
        TOKEN_MAP.put(JsonToken.VALUE_NULL, NULL);
        TOKEN_MAP.put(JsonToken.START_OBJECT, OBJECT);
        TOKEN_MAP.put(JsonToken.VALUE_STRING, STRING);

        final ImmutableMap.Builder<String, NodeType> builder = ImmutableMap.builder();

        for (final NodeType type : NodeType.values())
            builder.put(type.name, type);

        NAME_MAP = builder.build();
    }

    @Override
    public String toString() {
        return name;
    }

    public static NodeType fromName(final String name) {
        return NAME_MAP.get(name);
    }

    public static NodeType getNodeType(final JsonNode node) {
        final JsonToken token = node.asToken();
        final NodeType ret = TOKEN_MAP.get(token);

        Preconditions.checkNotNull(ret, "unhandled token type " + token);

        return ret;
    }
}
