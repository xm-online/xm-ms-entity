package com.icthh.xm.ms.entity.service;

import static com.jayway.jsonpath.Configuration.defaultConfiguration;
import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Component;

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
        sub.setVariableResolver(key -> {
            return String.valueOf(firstNonNull((Object) document.read(key), ""));
        });
        String result = sub.replace(template);
        log.debug("Template {}, params {}, result {}", template, json, result);
        return result;
    }
}
