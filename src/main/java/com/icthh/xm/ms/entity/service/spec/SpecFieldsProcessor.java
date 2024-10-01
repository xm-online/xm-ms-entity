package com.icthh.xm.ms.entity.service.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;

import static com.icthh.xm.ms.entity.validator.NodeType.OBJECT;
import static com.icthh.xm.ms.entity.validator.NodeType.getNodeType;

@Slf4j
public class SpecFieldsProcessor {

    @SneakyThrows
    public void processUniqueFields(Map<String, TypeSpec> types) {
        for (TypeSpec typeSpec: types.values()) {
            if (StringUtils.isBlank(typeSpec.getDataSpec())) {
                continue;
            }

            JsonNode node = new ObjectMapper().readTree(typeSpec.getDataSpec());
            Set<UniqueFieldSpec> uniqueFields = Sets.newHashSet();
            processNode(node, "$", uniqueFields);
            typeSpec.setUniqueFields(uniqueFields);
        }
        log.info("types.size={}", CollectionUtils.size(types));
    }

    private void processNode(JsonNode node, String jsonPath, Set<UniqueFieldSpec> uniqueFields) {
        if (node.has("unique") && node.get("unique").asBoolean()) {
            uniqueFields.add(new UniqueFieldSpec(jsonPath));
        }

        if (!isObject(node)) {
            return;
        }

        JsonNode properties = node.get("properties");
        properties.fieldNames().forEachRemaining(name -> processNode(properties.get(name), jsonPath + "." + name, uniqueFields));
    }

    private boolean isObject(JsonNode schemaNode) {
        return getNodeType(schemaNode).equals(OBJECT) && schemaNode.has("properties");
    }

}
