package com.icthh.xm.ms.entity.domain.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.boot.jackson.JsonObjectSerializer;

import java.io.IOException;
import java.time.Instant;

public class SimpleXmEntitySerializer extends JsonObjectSerializer<XmEntity> {

    private void write(JsonGenerator jsonGenerator, String field, Object value) throws IOException {
        if (value != null) {
            jsonGenerator.writeObjectField(field, value);
        }
    }

    private void writeInstant(JsonGenerator jsonGenerator, SerializerProvider provider, String field, Instant value) throws IOException {
        if (value != null) {
            jsonGenerator.writeFieldName(field);
            provider.findValueSerializer(Instant.class).serialize(value, jsonGenerator, provider);
        }
    }

    @Override
    protected void serializeObject(XmEntity value, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        write(jsonGenerator, "id", value.getId());
        write(jsonGenerator, "key", value.getKey());
        write(jsonGenerator, "typeKey", value.getTypeKey());
        write(jsonGenerator, "stateKey", value.getStateKey());
        write(jsonGenerator, "name", value.getName());
        writeInstant(jsonGenerator, provider, "startDate", value.getStartDate());
        writeInstant(jsonGenerator, provider, "updateDate", value.getUpdateDate());
        writeInstant(jsonGenerator, provider, "endDate", value.getEndDate());
        write(jsonGenerator, "avatarUrl", value.getAvatarUrl());
        write(jsonGenerator, "description", value.getDescription());
        write(jsonGenerator, "data", value.getData());
        write(jsonGenerator, "removed", value.isRemoved());
        write(jsonGenerator, "createdBy", value.getCreatedBy());
    }

}
