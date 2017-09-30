package com.icthh.xm.ms.entity.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public final class JsonUtils {

    private static ObjectMapper objectMapper;
    private final ObjectMapper objectMapper0;

    public static <T> T read(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("Error during reading json {}", json, e);
            return null;
        }
    }

    public static <T> String write(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            log.error("Error during writing json {}", object, e);
            return null;
        }
    }

    @PostConstruct
    private void postConstruct() {
        objectMapper = this.objectMapper0;
    }

}
