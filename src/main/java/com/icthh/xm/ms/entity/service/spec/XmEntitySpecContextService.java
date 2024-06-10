package com.icthh.xm.ms.entity.service.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.main.JsonSchema;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.FormSpecProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
@IgnoreLogginAspect
public class XmEntitySpecContextService {

    private final String maxFileSize;

    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;
    private final SpecInheritanceProcessor specInheritanceProcessor;
    private final DataSpecJsonSchemaService dataSpecJsonSchemaService;
    private final FunctionByTenantService functionByTenantService;
    private final SpecFieldsProcessor specFieldsProcessor;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, Map<String, TypeSpec>> typesByTenant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> typesByTenantByFile = new ConcurrentHashMap<>();

    public XmEntitySpecContextService(DefinitionSpecProcessor definitionSpecProcessor, FormSpecProcessor formSpecProcessor,
                                      XmEntityTenantConfigService tenantConfigService,
                                      @Value("${spring.servlet.multipart.max-file-size:1MB}") String maxFileSize) {
        this.definitionSpecProcessor = definitionSpecProcessor;
        this.formSpecProcessor = formSpecProcessor;
        this.specInheritanceProcessor = new SpecInheritanceProcessor(tenantConfigService);
        this.dataSpecJsonSchemaService = new DataSpecJsonSchemaService(definitionSpecProcessor, formSpecProcessor);
        this.specFieldsProcessor = new SpecFieldsProcessor();
        this.functionByTenantService = new FunctionByTenantService();
        this.maxFileSize = maxFileSize;
    }

    public Map<String, TypeSpec> typesByTenant(String tenantKey) {
        return nullSafe(typesByTenant.get(tenantKey));
    }

    public Map<String, TypeSpec> typesByTenantStrict(String tenantKey) {
        if (!typesByTenant.containsKey(tenantKey)) {
            log.error("Tenant configuration {} not found", tenantKey);
            throw new IllegalArgumentException("Tenant configuration not found");
        }
        return nullSafe(typesByTenant.get(tenantKey));
    }

    public Map<String, JsonSchema> dataSpecJsonSchemas(String tenantKey) {
        return dataSpecJsonSchemaService.dataSpecJsonSchemas(tenantKey);
    }

    public Map<String, FunctionSpec> functionsByTenant(String tenantKey) {
        return functionByTenantService.functionsByTenant(tenantKey);
    }

    public List<FunctionSpec> functionsByTenantOrdered(String tenantKey) {
        return functionByTenantService.functionsByTenantOrdered(tenantKey);
    }

    @SneakyThrows
    public void updateByFileState(String updatedKey, String config, String tenant) {
        var byFiles = typesByTenantByFile.computeIfAbsent(tenant, key -> new LinkedHashMap<>());
        if (StringUtils.isBlank(config)) {
            byFiles.remove(updatedKey);
            return;
        }

        byFiles.put(updatedKey, config);
    }

    public Map<String, TypeSpec> updateByTenantState(String tenant) {
        var tenantEntitySpec = readEntitySpec(tenant);

        definitionSpecProcessor.updateDefinitionStateByTenant(tenant, typesByTenantByFile);
        formSpecProcessor.updateFormStateByTenant(tenant, typesByTenantByFile);

        if (tenantEntitySpec.isEmpty()) {
            typesByTenant.remove(tenant);
        }

        typesByTenant.put(tenant, tenantEntitySpec);
        specInheritanceProcessor.process(tenantEntitySpec, tenant);
        specFieldsProcessor.processUniqueFields(tenantEntitySpec);

        functionByTenantService.processFunctionSpec(tenant, tenantEntitySpec);
        dataSpecJsonSchemaService.processDataSpec(tenant, tenantEntitySpec);

        return tenantEntitySpec;
    }

    private LinkedHashMap<String, TypeSpec> readEntitySpec(String tenant) {
        var tenantEntitySpec = new LinkedHashMap<String, TypeSpec>();
        typesByTenantByFile.get(tenant).values().stream()
            .map(this::toTypeSpecsMap)
            .forEach(tenantEntitySpec::putAll);
        return tenantEntitySpec;
    }

    @SneakyThrows
    private Map<String, TypeSpec> toTypeSpecsMap(String config) {
        XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);
        List<TypeSpec> typeSpecs = xmEntitySpec.getTypes();
        if (isEmpty(typeSpecs)) {
            return Collections.emptyMap();
        } else {
            // Convert List<TypeSpec> to Map<key, TypeSpec>
            Map<String, TypeSpec> result = typeSpecs.stream()
                .map(this::enrichAttachmentSpec)
                .collect(Collectors.toMap(TypeSpec::getKey, Function.identity(),
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    }, LinkedHashMap::new));
            return result;
        }
    }

    private TypeSpec enrichAttachmentSpec(TypeSpec typeSpec) {
        if (CollectionUtils.isEmpty(typeSpec.getAttachments())) {
            return typeSpec;
        }

        typeSpec.getAttachments().stream()
            .filter(it -> StringUtils.isEmpty(it.getSize()))
            .forEach(it -> it.setSize(maxFileSize));

        return typeSpec;
    }

}
