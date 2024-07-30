package com.icthh.xm.ms.entity.domain.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.hibernate.Hibernate;
import org.springframework.boot.jackson.JsonObjectSerializer;

import java.io.IOException;
import java.time.Instant;

public class SimpleXmEntitySerializer extends JsonObjectSerializer<XmEntity> {

    private void write(JsonGenerator jsonGenerator, SerializerProvider provider, String field, Object value)
    throws IOException {
        if (value != null) {
            provider.defaultSerializeField(field, value, jsonGenerator);
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
        if (!Hibernate.isInitialized(value)) {
            return;
        }

        write(jsonGenerator, provider, "id", value.getId());
        write(jsonGenerator, provider, "key", value.getKey());
        write(jsonGenerator, provider, "typeKey", value.getTypeKey());
        write(jsonGenerator, provider, "stateKey", value.getStateKey());
        write(jsonGenerator, provider, "name", value.getName());
        writeInstant(jsonGenerator, provider, "startDate", value.getStartDate());
        writeInstant(jsonGenerator, provider, "updateDate", value.getUpdateDate());
        writeInstant(jsonGenerator, provider, "endDate", value.getEndDate());
        write(jsonGenerator, provider, "avatarUrl", value.getAvatarUrl());
        write(jsonGenerator, provider, "description", value.getDescription());
        write(jsonGenerator, provider, "data", value.getData());
        write(jsonGenerator, provider, "removed", value.isRemoved());
        write(jsonGenerator, provider, "createdBy", value.getCreatedBy());
        write(jsonGenerator, provider, "updatedBy", value.getUpdatedBy());
    }

}
