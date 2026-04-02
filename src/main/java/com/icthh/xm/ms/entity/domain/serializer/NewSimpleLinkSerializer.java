package com.icthh.xm.ms.entity.domain.serializer;

import com.icthh.xm.ms.entity.domain.Link;
import java.time.Instant;
import org.hibernate.Hibernate;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class NewSimpleLinkSerializer extends StdSerializer<Link> {

    public NewSimpleLinkSerializer() {
        super(Link.class);
    }

    public NewSimpleLinkSerializer(Class<Link> t) {
        super(t);
    }

    private void write(JsonGenerator jsonGenerator, SerializationContext provider, String field, Object value) {
        if (value != null) {
            provider.defaultSerializeProperty(field, value, jsonGenerator);
        }
    }

    private void writeInstant(JsonGenerator jsonGenerator, SerializationContext provider, String field, Instant value) {
        if (value != null) {
            jsonGenerator.writeStringProperty(field, value.toString());
        }
    }

    @Override
    public void serialize(Link value, JsonGenerator jsonGenerator, SerializationContext provider) throws JacksonException {
        if (!Hibernate.isInitialized(value)) {
            return;
        }

        jsonGenerator.writeStartObject();

        write(jsonGenerator, provider, "id", value.getId());
        write(jsonGenerator, provider, "typeKey", value.getTypeKey());
        write(jsonGenerator, provider, "name", value.getName());
        write(jsonGenerator, provider, "description", value.getDescription());
        writeInstant(jsonGenerator, provider, "startDate", value.getStartDate());
        writeInstant(jsonGenerator, provider, "endDate", value.getEndDate());

        jsonGenerator.writeName("target");
        if (value.getTarget() != null) {
            jsonGenerator.writeStartObject();

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
            jsonGenerator.writeEndObject();
        } else {
            jsonGenerator.writeNullProperty("target");
        }

        if (value.getSource() != null) {
            write(jsonGenerator, provider, "source", value.getSource().getId());
        }

        jsonGenerator.writeEndObject();
    }
}
