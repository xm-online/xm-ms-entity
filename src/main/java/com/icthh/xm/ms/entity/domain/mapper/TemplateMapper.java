package com.icthh.xm.ms.entity.domain.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.ms.entity.domain.template.Template;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@UtilityClass
public class TemplateMapper {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public Map<String, Template> ymlToTemplates(String yml) {
        try {
            Map<String, Template> map = mapper
                .readValue(yml, new TypeReference<TreeMap<String, Template>>() {
                });
            map.forEach((templateKey, template) -> template.setKey(templateKey));
            return map;
        } catch (Exception e) {
            log.error("Failed to create template collection from YML file, error: {}", e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
