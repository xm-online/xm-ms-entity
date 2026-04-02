package com.icthh.xm.ms.entity.domain.converter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, Object>, String> {

    private ObjectMapper mapper = JsonMapper.builder().build();

    public MapToStringConverter() {
    }

    @Override
    public String convertToDatabaseColumn(Map<String, Object> data) {
        try {
            return mapper.writeValueAsString(data != null ? data : new HashMap<>());
        } catch (JacksonException e) {
            log.warn("Error during JSON to String converting", e);
            return "";
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String data) {
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
        };
        try {
            return mapper.readValue(StringUtils.isNoneBlank(data) ? data : "{}", typeRef);
        } catch (JacksonException e) {
            log.warn("Error during String to JSON converting", e);
            return Collections.emptyMap();
        }
    }

}
