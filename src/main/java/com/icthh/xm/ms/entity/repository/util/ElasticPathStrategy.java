package com.icthh.xm.ms.entity.repository.util;

import com.fasterxml.jackson.contrib.jsonpath.JsonUnmarshaller;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.icthh.xm.ms.entity.contrib.jsonpath.FixedDefaultJsonUnmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component("elasticPath")
public class ElasticPathStrategy implements MappingStrategy {

    @Override
    public <T> T map(String json, Class<T> clazz) {
        if (null == json) {
            throw new IllegalArgumentException("Can not map empty json");
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        JsonUnmarshaller jsonUnmarshaller = new FixedDefaultJsonUnmarshaller(mapper);
        try {
            return jsonUnmarshaller.unmarshal(clazz, json);
        } catch (IOException e) {
            log.warn("Error during mapping", e);
            return null;
        }
    }
}
