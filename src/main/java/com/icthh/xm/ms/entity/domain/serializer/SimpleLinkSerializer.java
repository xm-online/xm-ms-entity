package com.icthh.xm.ms.entity.domain.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.icthh.xm.ms.entity.domain.Link;
import org.hibernate.Hibernate;
import org.springframework.boot.jackson.JsonObjectSerializer;

import java.io.IOException;
import java.time.Instant;

public class SimpleLinkSerializer extends JsonObjectSerializer<Link> {

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
    protected void serializeObject(Link value, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
        if (!Hibernate.isInitialized(value)) {
            return;
        }

        write(jsonGenerator, provider, "id", value.getId());
        write(jsonGenerator, provider, "typeKey", value.getTypeKey());
        write(jsonGenerator, provider, "name", value.getName());
        write(jsonGenerator, provider, "description", value.getDescription());
        writeInstant(jsonGenerator, provider, "startDate", value.getStartDate());
        writeInstant(jsonGenerator, provider, "endDate", value.getEndDate());

        jsonGenerator.writeObjectFieldStart("target");
        if (value.getTarget() != null) {
            write(jsonGenerator, provider, "id", value.getTarget().getId());
            write(jsonGenerator, provider, "key", value.getTarget().getKey());
            write(jsonGenerator, provider, "typeKey", value.getTarget().getTypeKey());
            write(jsonGenerator, provider, "stateKey", value.getTarget().getStateKey());
            write(jsonGenerator, provider, "name", value.getTarget().getName());
            writeInstant(jsonGenerator, provider, "startDate", value.getTarget().getStartDate());
            writeInstant(jsonGenerator, provider, "endDate", value.getTarget().getEndDate());
            writeInstant(jsonGenerator, provider, "updateDate", value.getTarget().getUpdateDate());
            write(jsonGenerator, provider, "avatarUrl", value.getTarget().getAvatarUrl());
            write(jsonGenerator, provider, "description", value.getTarget().getDescription());
            write(jsonGenerator, provider, "createdBy", value.getTarget().getCreatedBy());
            write(jsonGenerator, provider, "removed", value.getTarget().isRemoved());
            write(jsonGenerator, provider, "data", value.getTarget().getData());
        }
        jsonGenerator.writeEndObject();

        if (value.getSource() != null) {
            write(jsonGenerator, provider, "source", value.getSource().getId());
        }
    }
}
