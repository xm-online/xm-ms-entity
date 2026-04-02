package com.icthh.xm.ms.entity.domain.serializer;

import com.icthh.xm.ms.entity.domain.XmEntity;
import java.time.Instant;
import org.hibernate.Hibernate;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class NewSimpleXmEntitySerializer extends StdSerializer<XmEntity> {

    public NewSimpleXmEntitySerializer() {
        super(XmEntity.class);
    }

    public NewSimpleXmEntitySerializer(Class<XmEntity> t) {
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
    public void serialize(XmEntity value, JsonGenerator jsonGenerator, SerializationContext provider) throws JacksonException {
        if (!Hibernate.isInitialized(value)) {
            return;
        }

        jsonGenerator.writeStartObject();

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

        jsonGenerator.writeEndObject();
    }
}
