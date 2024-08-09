package com.icthh.xm.ms.entity.service;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.apache.commons.text.StringSubstitutor.DEFAULT_VAR_DEFAULT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleTemplateProcessor {

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public String processTemplate(String template, Object object) {
        String json = objectMapper.writeValueAsString(object);
        DocumentContext document = JsonPath.using(defaultConfiguration().addOptions(SUPPRESS_EXCEPTIONS)).parse(json);
        StringSubstitutor sub = new StringSubstitutor();
        sub = sub.setDisableSubstitutionInValues(true).setEnableSubstitutionInVariables(false);
        sub.setValueDelimiter(UUID.randomUUID().toString());
        sub.setVariableResolver(key -> {
            String defaultValue = "";
            if (key.contains(DEFAULT_VAR_DEFAULT)) {
                String[] parts = key.split(DEFAULT_VAR_DEFAULT);
                key = parts[0];
                if (parts.length > 1) {
                    defaultValue = parts[1];
                }
            }
            Object value = document.read(key.trim());
            return value != null ? value.toString() : defaultValue;
        });
        String result = sub.replace(template);
        log.debug("Template {}, params {}, result {}", template, json, result);
        return result;
    }
}
