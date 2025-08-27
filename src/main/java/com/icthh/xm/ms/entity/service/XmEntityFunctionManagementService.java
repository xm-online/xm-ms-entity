package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.utils.YamlPatchUtils.addObject;
import static com.icthh.xm.commons.utils.YamlPatchUtils.addSequenceItem;
import static com.icthh.xm.commons.utils.YamlPatchUtils.arrayByField;
import static com.icthh.xm.commons.utils.YamlPatchUtils.delete;
import static com.icthh.xm.commons.utils.YamlPatchUtils.key;
import static com.icthh.xm.commons.utils.YamlPatchUtils.updateSequenceItem;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.service.FunctionManageService;
import com.icthh.xm.commons.utils.YamlPatchUtils.YamlPatchPattern;
import com.icthh.xm.ms.entity.domain.function.FunctionSpecDto;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.service.spec.FunctionMetaInfo;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class XmEntityFunctionManagementService implements FunctionManageService<FunctionSpec, FunctionSpecDto> {

    private final XmEntitySpecService xmEntitySpecService;
    private final CommonConfigRepository commonConfigRepository;
    private final CommonConfigService commonConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    @Override
    public void addFunction(FunctionSpecDto newFunction) {
        assertKeyUnique(newFunction);
        String entityTypeKey = newFunction.getEntityTypeKey();
        Configuration config = xmEntitySpecService.getFileContextByEntityTypeKey(entityTypeKey);
        FunctionSpec item = newFunction.getItem();
        String yaml = config.getContent();

        yaml = addToYaml(yaml, entityTypeKey, item);

        Configuration updatedConfig = new Configuration(config.getPath(), yaml);
        commonConfigRepository.updateConfigFullPath(updatedConfig, null);
        commonConfigService.notifyUpdated(updatedConfig);
    }

    private String addToYaml(String yaml, String entityTypeKey, FunctionSpec item) {
        if (!jsonPathExists(yaml, "$.types[?(@.key=='" + entityTypeKey + "')].functions")) {
            yaml = addObject(yaml,
                Map.of("functions", List.of(item)),
                List.of(key("types"), arrayByField(Map.of("key", entityTypeKey))));
        } else {
            yaml = addSequenceItem(yaml, item, addPath(entityTypeKey));
        }
        return yaml;
    }

    private List<YamlPatchPattern> addPath(String entityTypeKey) {
        return List.of(key("types"), arrayByField(Map.of("key", entityTypeKey)), key("functions"));
    }

    private List<YamlPatchPattern> deletePath(String entityTypeKey, String functionKey) {
        return List.of(key("types"), arrayByField(Map.of("key", entityTypeKey)),
            key("functions"), arrayByField(Map.of("key", functionKey)));
    }

    @Override
    public void updateFunction(FunctionSpecDto updatedFunction) {
        assertKeyExists(updatedFunction);

        String functionKey = updatedFunction.getItem().getKey();
        List<String> entityTypeKeys = findByKey(functionKey).stream().map(FunctionMetaInfo::entityTypeKey).toList();
        String entityTypeKey = entityTypeKeys.getFirst();

        if (entityTypeKeys.size() == 1 && Objects.equals(entityTypeKey, updatedFunction.getEntityTypeKey())) {
            Configuration config = xmEntitySpecService.getFileContextByEntityTypeKey(entityTypeKey);
            String updatedYaml = updateSequenceItem(config.getContent(), updatedFunction.getItem(), deletePath(entityTypeKey, functionKey));
            Configuration updatedConfig = new Configuration(config.getPath(), updatedYaml);
            commonConfigRepository.updateConfigFullPath(updatedConfig, null);
            commonConfigService.notifyUpdated(updatedConfig);
        } else {
            removeFunction(functionKey);
            addFunction(updatedFunction);
        }
    }

    @Override
    public void removeFunction(String functionKey) {
        List<String> entityTypeKeys = findByKey(functionKey).stream().map(FunctionMetaInfo::entityTypeKey).toList();
        entityTypeKeys.forEach(entityTypeKey -> {
            Configuration config = xmEntitySpecService.getFileContextByEntityTypeKey(entityTypeKey);
            String yaml = config.getContent();
            yaml = delete(yaml, deletePath(entityTypeKey, functionKey));
            Configuration updatedConfig = new Configuration(config.getPath(), yaml);
            commonConfigRepository.updateConfigFullPath(updatedConfig, null);
            commonConfigService.notifyUpdated(updatedConfig);
        });
    }

    private List<FunctionMetaInfo> findByKey(String functionKey) {
        return xmEntitySpecService.findFunctionMetaInfoByKey(functionKey);
    }

    @Override
    public TypeReference<FunctionSpecDto> getFunctionSpecWrapperClass() {
        return new TypeReference<>() {};
    }

    @Override
    public TypeReference<FunctionSpec> getFunctionSpecClass() {
        return new TypeReference<>() {};
    }

    private void assertKeyUnique(FunctionSpecWithFileName<FunctionSpec> newFunction) {
        if (xmEntitySpecService.findFunctionByKey(newFunction.getItem().getKey()) != null) {
            log.error("Function with key '{}' already exists", newFunction.getItem().getKey());
            throw new BusinessException("error.function.with.key.already.exists", "Function already exists");
        }
    }

    private void assertKeyExists(FunctionSpecWithFileName<FunctionSpec> updatedFunction) {
        if (xmEntitySpecService.findFunctionByKey(updatedFunction.getItem().getKey()) == null) {
            throw functionNotFound(updatedFunction.getItem().getKey());
        }
    }

    private static BusinessException functionNotFound(String functionKey) {
        log.error("Function with key '{}' not exists", functionKey);
        return new BusinessException("error.function.with.key.not.exists", "Function not exists");
    }

    @SneakyThrows
    public boolean jsonPathExists(String yaml, String jsonPath) {
        try {
            Map<String, Object> map = objectMapper.readValue(yaml, Map.class);
            Object result = JsonPath.read(map, jsonPath);
            if (result == null) {
                return false;
            }
            if (result instanceof Iterable) {
                return ((Iterable<?>) result).iterator().hasNext();
            }
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }
}
