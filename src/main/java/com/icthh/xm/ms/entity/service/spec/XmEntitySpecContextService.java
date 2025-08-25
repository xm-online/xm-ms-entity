package com.icthh.xm.ms.entity.service.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.networknt.schema.JsonSchema;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.icthh.xm.ms.entity.service.json.JsonConfigurationListener.XM_ENTITY_SPEC_KEY;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@LepService(group = "service.spec")
@IgnoreLogginAspect
public class XmEntitySpecContextService {

    private final String maxFileSize;

    private final SpecInheritanceProcessor specInheritanceProcessor;
    private final DataSpecJsonSchemaService dataSpecJsonSchemaService;
    private final FunctionByTenantService functionByTenantService;
    private final SpecFieldsProcessor specFieldsProcessor;
    private final XmEntitySpecCustomizer xmEntitySpecCustomizer;
    // workaround we need redesign how RefreshableConfiguration inited, and remove BeanPostProcessor
    // now we have cycle entitySpecService -> EntityService -> LepContextFactory -> <lep-s related> -> entitySpecCustomizer -> entitySpecService
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, Map<String, TypeSpec>> typesByTenant = new ConcurrentHashMap<>();
    // tenant -> filePath -> fileContent
    private final ConcurrentHashMap<String, Map<String, String>> typesByTenantByFile = new ConcurrentHashMap<>();
    // tenant -> entity typeKey -> filePath
    private final ConcurrentHashMap<String, Map<String, String>> tenantToTypeToFile = new ConcurrentHashMap<>();

    public XmEntitySpecContextService(DataSpecJsonSchemaService dataSpecJsonSchemaService,
                                      XmEntitySpecCustomizer xmEntitySpecCustomizer,
                                      XmEntityTenantConfigService tenantConfigService,
                                      @Value("${spring.servlet.multipart.max-file-size:1MB}") String maxFileSize) {
        this.xmEntitySpecCustomizer = xmEntitySpecCustomizer;
        this.specInheritanceProcessor = new SpecInheritanceProcessor(tenantConfigService);
        this.dataSpecJsonSchemaService = dataSpecJsonSchemaService;
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

    public List<FunctionMetaInfo> functionsMetaInfoByTenant(String tenantKey) {
        return functionByTenantService.functionsMetaInfoByTenant(tenantKey);
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

    public List<String> tenants() {
        return new ArrayList<>(typesByTenantByFile.keySet());
    }

    public Map<String, TypeSpec> updateByTenantState(String tenant) {
        Map<String, XmEntitySpecification> xmEntitySpecificationByFiles = readXmEntitySpecTypes(tenant);
        LinkedHashMap<String, TypeSpec> tenantEntitySpec = getTenantEntitySpecs(xmEntitySpecificationByFiles);
        Map<String, String> entityKeyToFile = entityKeyToFile(xmEntitySpecificationByFiles);
        if (initialized.get()) {
            xmEntitySpecCustomizer.customize(tenant, tenantEntitySpec);
        }

        var xmEntitySpecs = xmEntitySpecificationByFiles.values().stream().map(XmEntitySpecification::spec).toList();
        dataSpecJsonSchemaService.updateByTenantState(tenant, XM_ENTITY_SPEC_KEY, xmEntitySpecs);

        if (tenantEntitySpec.isEmpty()) {
            typesByTenant.remove(tenant);
        }

        specInheritanceProcessor.process(tenantEntitySpec, tenant);
        specFieldsProcessor.processUniqueFields(tenantEntitySpec);

        functionByTenantService.processFunctionSpec(tenant, xmEntitySpecificationByFiles);

        xmEntitySpecs.forEach(spec -> dataSpecJsonSchemaService.processSpecification(tenant, XM_ENTITY_SPEC_KEY, spec));

        typesByTenant.put(tenant, tenantEntitySpec);
        tenantToTypeToFile.put(tenant, entityKeyToFile);

        return tenantEntitySpec;
    }

    public Optional<Configuration> getFileContextByEntityTypeKey(String tenant, String typeKey) {
        return ofNullable(tenantToTypeToFile.get(tenant))
            .map(it -> it.get(typeKey))
            .flatMap(filePath -> ofNullable(typesByTenantByFile.get(tenant))
                .map(it -> it.get(filePath))
                .map(content -> new Configuration(filePath, content)));
    }

    private Map<String, String> entityKeyToFile(Map<String, XmEntitySpecification> xmEntitySpecificationByFiles) {
        Map<String, String> entityKeyToFile = new HashMap<>();
        xmEntitySpecificationByFiles.values().stream()
            .filter(it -> nonNull(it.types()))
            .forEach(spec -> spec.types().values().forEach(type ->
                entityKeyToFile.put(type.getKey(), spec.filePath())
            ));
        return entityKeyToFile;
    }

    private LinkedHashMap<String, TypeSpec> getTenantEntitySpecs(
        Map<String, XmEntitySpecification> xmEntitySpecTypesMap) {
        return xmEntitySpecTypesMap.values().stream()
            .filter(it -> nonNull(it.types()))
            .flatMap(it -> it.types().entrySet().stream())
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
    }

    private Map<String, XmEntitySpecification> readXmEntitySpecTypes(String tenant) {
        return typesByTenantByFile.get(tenant).entrySet().stream()
            .map(it -> toXmEntitySpecTypes(it.getKey(), it.getValue()))
            .collect(toMap(XmEntitySpecification::filePath, identity()));
    }

    @SneakyThrows
    private XmEntitySpecification toXmEntitySpecTypes(String path, String config) {
        XmEntitySpec xmEntitySpec = mapper.readValue(config, XmEntitySpec.class);
        // Convert List<TypeSpec> to Map<key, TypeSpec>
        LinkedHashMap<String, TypeSpec> result = ofNullable(xmEntitySpec.getTypes())
            .orElse(List.of())
            .stream()
            .map(this::enrichAttachmentSpec)
            .collect(toMap(TypeSpec::getKey, identity(),
                (u, v) -> {
                    throw new IllegalStateException(String.format("Duplicate key %s", u));
                }, LinkedHashMap::new));
        return new XmEntitySpecification(path, xmEntitySpec, result);
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

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if(initialized.compareAndSet(false, true)) {
            tenants().forEach(this::updateByTenantState);
        }
    }

    public List<DefinitionSpec> getDefinitions(String tenantKeyValue) {
        return dataSpecJsonSchemaService.getDefinitions(tenantKeyValue, XM_ENTITY_SPEC_KEY);
    }
}
