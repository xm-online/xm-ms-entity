package com.icthh.xm.ms.entity.service;

import static com.github.fge.jackson.NodeType.OBJECT;
import static com.github.fge.jackson.NodeType.getNodeType;
import static com.google.common.collect.Iterables.getFirst;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.ACCESS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.ATTACHMENTS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.CALENDARS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.FUNCTIONS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.LINKS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.LOCATIONS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.RATINGS;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.STATES;
import static com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter.TAGS;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;
import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.union;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.ext.TypeSpecParameter;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
import com.icthh.xm.ms.entity.domain.spec.EventSpec;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.RatingSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.icthh.xm.ms.entity.service.processor.DefinitionSpecProcessor;
import com.icthh.xm.ms.entity.service.processor.FormSpecProcessor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/**
 * XM Entity Specification service, that provides extra possibilities to
 * manipulate with pure model.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntitySpecService implements RefreshableConfiguration {

    private static final String TYPE_SEPARATOR_REGEXP = "\\.";
    private static final String TYPE_SEPARATOR = ".";
    private static final String TENANT_NAME = "tenantName";
    private static final String XM_ENTITY_INHERITANCE_DEFINITION = "xmEntityInheritanceDefinition";
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, Map<String, TypeSpec>> typesByTenant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, String>> typesByTenantByFile = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, com.github.fge.jsonschema.main.JsonSchema>> dataSpecJsonSchemas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, FunctionSpec>> functionsByTenant = new ConcurrentHashMap<>();

    private final TenantConfigRepository tenantConfigRepository;
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;
    private final List<EntitySpecUpdateListener> entitySpecUpdateListeners;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;
    private final XmEntityTenantConfigService tenantConfigService;
    private final DefinitionSpecProcessor definitionSpecProcessor;
    private final FormSpecProcessor formSpecProcessor;

    /**
     * Search of all entity Type specifications.
     * @return list of entity Types specifications
     */
    public List<TypeSpec> findAllTypes() {
        return getTypeSpecs().values().stream().map(this::filterFunctions).collect(Collectors.toList());
    }

    /**
     * Search of all entity Type specifications that marked as application.
     * @return list of entity Types specifications that defines as application.
     */
    public List<TypeSpec> findAllAppTypes() {
        return getTypeSpecs().values().stream().filter(isApp()).map(this::filterFunctions).collect(Collectors.toList());
    }

    /**
     * Search of all not an abstract entity Type specifications.
     * @return list of entity Types specifications that not an abstract.
     */
    public List<TypeSpec> findAllNonAbstractTypes() {
        return getTypeSpecs().values().stream().filter(isNotAbstract()).map(this::filterFunctions).collect(Collectors.toList());
    }

    public static Predicate<TypeSpec> isApp() {
        return TypeSpec::getIsApp;
    }

    public static Predicate<TypeSpec> isNotAbstract() {
        return t -> !t.getIsAbstract();
    }


    @Override
    @SneakyThrows
    @IgnoreLogginAspect
    public void onRefresh(String updatedKey, String config) {

        try {
            String tenant = extractTenantName(updatedKey);
            updateByFileState(updatedKey, config, tenant);
            updateByTenantState(tenant);
            log.info("Specification was for tenant {} updated from file {}", tenant, updatedKey);
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        Set<String> tenants = paths.stream().map(this::extractTenantName).collect(Collectors.toSet());
        tenants.forEach(tenantKey -> {
            Map<String, TypeSpec> tenantEntitySpec = nullSafe(typesByTenant.get(tenantKey));
            entitySpecUpdateListeners.forEach(it -> it.onEntitySpecUpdate(tenantEntitySpec, tenantKey));
        });
    }

    private String extractTenantName(String it) {
        return matcher.extractUriTemplateVariables(getPathPattern(it), it).get(TENANT_NAME);
    }

    private String getPathPattern(String updatedKey) {
        String specificationPattern = applicationProperties.getSpecificationPathPattern();
        String specificationFolderPattern = applicationProperties.getSpecificationFolderPathPattern();
        if (matcher.match(specificationPattern, updatedKey)) {
            return specificationPattern;
        } else if (matcher.match(specificationFolderPattern, updatedKey)) {
            return specificationFolderPattern;
        }
        throw new IllegalStateException("Config path does not match defined patterns");
    }

    private Map<String, TypeSpec> updateByTenantState(String tenant) {
        var tenantEntitySpec = new LinkedHashMap<String, TypeSpec>();
        typesByTenantByFile.get(tenant).values().stream().map(this::toTypeSpecsMap).forEach(tenantEntitySpec::putAll);
        definitionSpecProcessor.updateDefinitionStateByTenant(tenant, typesByTenantByFile);
        formSpecProcessor.updateFormStateByTenant(tenant, typesByTenantByFile);

        if (tenantEntitySpec.isEmpty()) {
            typesByTenant.remove(tenant);
        }
        typesByTenant.put(tenant, tenantEntitySpec);
        inheritance(tenantEntitySpec, tenant);
        processUniqueFields(tenantEntitySpec);

        var functionSpec = tenantEntitySpec.values().stream()
                .map(TypeSpec::getFunctions)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(FunctionSpec::getKey, fs -> fs, (t, t2) -> t));
        if (functionSpec.isEmpty()) {
            functionsByTenant.remove(tenant);
        }
        functionsByTenant.put(tenant, functionSpec);

        var dataSchemas = new HashMap<String, com.github.fge.jsonschema.main.JsonSchema>();
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
        for (TypeSpec typeSpec : tenantEntitySpec.values()) {
            processTypeSpec(tenant, typeSpec);
            addJsonSchema(dataSchemas, jsonSchemaFactory, typeSpec);
        }
        dataSpecJsonSchemas.put(tenant, dataSchemas);
        return tenantEntitySpec;
    }

    private void processTypeSpec(String tenant, TypeSpec typeSpec) {
        definitionSpecProcessor.processTypeSpec(tenant, typeSpec::setDataSpec, typeSpec::getDataSpec);
        formSpecProcessor.processTypeSpec(tenant, typeSpec::setDataForm, typeSpec::getDataForm);

        Stream.ofNullable(typeSpec.getStates())
            .flatMap(Collection::stream)
            .map(StateSpec::getNext)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .forEach(nextSpec -> {
                definitionSpecProcessor.processTypeSpec(tenant, nextSpec::setInputSpec, nextSpec::getInputSpec);
                formSpecProcessor.processTypeSpec(tenant,nextSpec::setInputForm, nextSpec::getInputForm);
            });

        Stream.ofNullable(typeSpec.getFunctions())
            .flatMap(Collection::stream)
            .forEach(functionSpec -> {
                definitionSpecProcessor.processTypeSpec(tenant, functionSpec::setInputSpec, functionSpec::getInputSpec);
                definitionSpecProcessor.processTypeSpec(tenant, functionSpec::setContextDataSpec, functionSpec::getContextDataSpec);
                formSpecProcessor.processTypeSpec(tenant,functionSpec::setInputForm, functionSpec::getInputForm);
                formSpecProcessor.processTypeSpec(tenant,functionSpec::setContextDataForm, functionSpec::getContextDataForm);
            });
    }

    private void addJsonSchema(HashMap<String, com.github.fge.jsonschema.main.JsonSchema> dataSchemas,
                               JsonSchemaFactory jsonSchemaFactory, TypeSpec typeSpec) {
        if (StringUtils.isNotBlank(typeSpec.getDataSpec())) {
            try {
                var jsonSchema = jsonSchemaFactory.getJsonSchema(JsonLoader.fromString(typeSpec.getDataSpec()));
                dataSchemas.put(typeSpec.getKey(), jsonSchema);
            } catch (IOException | ProcessingException e) {
                log.error("Error processing data spec", e);
            }
        }
    }

    @SneakyThrows
    private void updateByFileState(String updatedKey, String config, String tenant) {
        var byFiles = typesByTenantByFile.computeIfAbsent(tenant, key -> new LinkedHashMap<>());
        if (StringUtils.isBlank(config)) {
            byFiles.remove(updatedKey);
            return;
        }

        byFiles.put(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPattern = applicationProperties.getSpecificationPathPattern();
        String specificationFolderPattern = applicationProperties.getSpecificationFolderPathPattern();
        return matcher.match(specificationPattern, updatedKey) || matcher.match(specificationFolderPattern, updatedKey);
    }

    @Override
    @LoggingAspectConfig(inputExcludeParams = "config")
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    @SneakyThrows
    @LoggingAspectConfig(inputExcludeParams = "xmEntitySpecString")
    public void updateXmEntitySpec(String xmEntitySpecString) {
        String configName = applicationProperties.getSpecificationName();

        // Simple validation correct structure.
        XmEntitySpec xmEntitySpec = mapper.readValue(xmEntitySpecString, XmEntitySpec.class);

        tenantConfigRepository.updateConfig(getTenantKeyValue(), "/" + configName, xmEntitySpecString);
    }

    /**
     * Assumption value of types.get(tenantKey) could be null
     * @return map with nullSafe check
     */
    protected Map<String, TypeSpec> getTypeSpecs() {
        String tenantKeyValue = getTenantKeyValue();
        if (!typesByTenant.containsKey(tenantKeyValue)) {
            log.error("Tenant configuration {} not found", tenantKeyValue);
            throw new IllegalArgumentException("Tenant configuration not found");
        }
        return nullSafe(typesByTenant.get(tenantKeyValue));
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<TypeSpec> getTypeSpecByKey(String key) {
        return ofNullable(getTypeSpecs().get(key)).map(this::filterFunctions);
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<com.github.fge.jsonschema.main.JsonSchema> getDataJsonSchemaByKey(String key) {
        return ofNullable(dataSpecJsonSchemas.get(getTenantKeyValue())).map(it -> it.get(key));
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<TypeSpec> getTypeSpecByKeyWithoutFunctionFilter(String key) {
        return ofNullable(getTypeSpecs().get(key));
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<LinkSpec> getLinkSpec(String entityTypeKey, String linkTypeKey) {
        return getTypeSpecByKey(entityTypeKey).flatMap(ts -> ts.findLinkSpec(linkTypeKey));
    }

    /**
     * Search entity Type specification by key.
     *
     * @param key entity Type specification key
     * @return instance of entity Type specification if present
     * @Deprecation method could return null, please use getTypeSpecByKey instead. findTypeByKey will be refactored
     */
    @IgnoreLogginAspect
    @Deprecated
    public TypeSpec findTypeByKey(String key) {
        return getTypeSpecs().get(key);
    }


    /**
     * Search of real Type specifications based on Type key prefix. For example:
     * with prefix `TYPE1` search, the key with `TYPE1.SUBTYPE1` should be
     * returned if `TYPE1.SUBTYPE1` is not an abstract.
     * If prefix is empty string, search return all not an abstract entity Type specifications.
     *
     * @param prefix search prefix
     * @return list of entity Types specifications
     */
    public List<TypeSpec> findNonAbstractTypesByPrefix(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            return findAllNonAbstractTypes();
        }
        return findAllNonAbstractTypes().stream().filter(bySpecTypesPrefix(prefix)).collect(Collectors.toList());
    }

    private Predicate<TypeSpec> bySpecTypesPrefix(String prefix) {
        return t -> t.getKey().equals(prefix) || t.getKey().startsWith(prefix + TYPE_SEPARATOR);
    }

    /**
     * Search of the first Attachment specification in the Type specification.
     *
     * @param key           entity Type specification key
     * @param attachmentKey Attachment key
     * @return entity Attachment if present
     */
    @LoggingAspectConfig(resultDetails = false)
    public Optional<AttachmentSpec> findAttachment(String key, String attachmentKey) {
        return getTypeSpecs().get(key).getAttachments().stream().filter(l -> l.getKey().equals(attachmentKey))
            .findFirst();
    }

    /**
     * Search of the first Link specification in the Type specification.
     *
     * @param key     entity Type specification key
     * @param linkKey Link key
     * @return entity Link if present
     */
    @LoggingAspectConfig(resultDetails = false)
    public Optional<LinkSpec> findLink(String key, String linkKey) {
        return getTypeSpecs().get(key).getLinks().stream().filter(l -> l.getKey().equals(linkKey)).findFirst();
    }

    /**
     * Search of the first Location specification in the Type specification.
     *
     * @param key         entity Type specification key
     * @param locationKey Location key
     * @return entity Location if present
     */
    @LoggingAspectConfig(resultDetails = false)
    public Optional<LocationSpec> findLocation(String key, String locationKey) {
        return getTypeSpecs().get(key).getLocations().stream().filter(l -> l.getKey().equals(locationKey))
            .findFirst();
    }

    /**
     * Search of the first Rating specification in the Type specification.
     *
     * @param key       entity Type specification key
     * @param ratingKey Rating key
     * @return entity Rating if present
     */
    @LoggingAspectConfig(resultDetails = false)
    public Optional<RatingSpec> findRating(String key, String ratingKey) {
        return getTypeSpecs().get(key).getRatings().stream().filter(l -> l.getKey().equals(ratingKey)).findFirst();
    }

    /**
     * Search of the first State specification in the Type specification.
     *
     * @param key      entity Type specification key
     * @param stateKey State key
     * @return entity State if present
     */
    @LoggingAspectConfig(resultDetails = false)
    public Optional<StateSpec> findState(String key, String stateKey) {
        return getTypeSpecs().get(key).getStates().stream().filter(s -> s.getKey().equals(stateKey)).findFirst();
    }

    @IgnoreLogginAspect
    public Optional<FunctionSpec> findFunction(String functionKey) {

        Map<String, FunctionSpec> functions = nullSafe(functionsByTenant.get(getTenantKeyValue()));
        return Optional.of(functions)
                .map(fs -> fs.get(functionKey))
                .or(() -> functions.values().stream()
                        .filter(fs -> fs.getPath() != null)
                        .filter(fs -> matcher.match(fs.getPath(), functionKey))
                        .findFirst());

    }

    @IgnoreLogginAspect
    public Optional<FunctionSpec> findFunction(String typeKey, String functionKey) {
        Predicate<FunctionSpec> keysEquals = fs -> StringUtils.equals(fs.getKey(), functionKey);

        List<FunctionSpec> functionSpecs = getTypeSpecByKey(typeKey)
            .map(TypeSpec::getFunctions)
            .orElse(emptyList());

        return functionSpecs.stream().filter(keysEquals).findFirst();
    }

    /**
     * Find {@link EventSpec} for specified event type key.
     * @implNote method throws exception in case when more than one {@link EventSpec} found
     * @param eventTypeKey the event type key to find
     * @return the event specification
     */
    @IgnoreLogginAspect
    public Optional<EventSpec> findEvent(String eventTypeKey) {
        List<EventSpec> eventSpecs = Stream.ofNullable(getTypeSpecs())
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(TypeSpec::getCalendars)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .map(CalendarSpec::getEvents)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(eventSpec -> eventTypeKey.equals(eventSpec.getKey()))
            .collect(Collectors.toList());
        if (eventSpecs.size() > 1) {
            throw new IllegalStateException("Found more than one Event specifications by key:" + eventTypeKey);
        }
        return ofNullable(getFirst(eventSpecs, null));
    }

    /**
     * Find {@link CalendarSpec} for specified calendar type key.
     * @implNote method throws exception in case when more than one {@link Calendar} found
     * @param calendarTypeKey the calendar type key to find
     * @return the calendar specification
     */
    @IgnoreLogginAspect
    public Optional<CalendarSpec> findCalendar(String typeKey, String calendarTypeKey) {
        return getTypeSpecByKey(typeKey)
            .map(TypeSpec::getCalendars)
            .stream()
            .flatMap(Collection::stream)
            .filter(calendarSpec -> calendarSpec.getKey().equals(calendarTypeKey))
            .findFirst();
    }

    /**
     * Search possible next steps for entity in the Type specification.
     *
     * @param key      entity Type specification key
     * @param stateKey current entity State key, if null all states will be returned
     * @return entity next steps if present
     */
    public List<NextSpec> next(String key, String stateKey) {
        if (isEmpty(getTypeSpecs().get(key).getStates())) {
            return emptyList();
        }

        if (stateKey == null) {
            return getTypeSpecs().get(key).getStates().stream().map(state -> NextSpec.builder().stateKey(state.getKey())
                .build()).collect(Collectors.toList());
        }
        Optional<StateSpec> state = findState(key, stateKey);
        if (state.isPresent()) {
            List<NextSpec> next = state.get().getNext();
            return next != null ? next : emptyList();
        } else {
            return emptyList();
        }
    }

    /**
     * Search possible next States specifications for entity in the Type
     * specification.
     *
     * @param key      entity Type specification key
     * @param stateKey current entity State key
     * @return entity next States if present
     */
    public List<StateSpec> nextStates(String key, String stateKey) {
        return next(key, stateKey).stream().map(n -> findState(key, n.getStateKey()).get())
            .collect(Collectors.toList());
    }

    /**
     * Search of the first Tag specification in the Type specification.
     *
     * @param key    entity Type specification key
     * @param tagKey Tag key
     * @return entity Tag if present
     */
    @LoggingAspectConfig(resultDetails = false)
    public Optional<TagSpec> findTag(String key, String tagKey) {
        return getTypeSpecs().get(key).getTags().stream().filter(l -> l.getKey().equals(tagKey)).findFirst();
    }

    /**
     * Transforms all XmEntity Specification keys into the thin structure based
     * in maps and sets.
     *
     * @return thin structure based in maps and sets
     */
    @IgnoreLogginAspect
    public Map<String, Map<String, Set<String>>> getAllKeys() {
        Map<String, Map<String, Set<String>>> result = Maps.newHashMap();
        for (TypeSpec typeSpec : findAllTypes()) {
            Map<String, Set<String>> subKeys = Maps.newHashMap();
            getKeys(subKeys, AttachmentSpec.class, typeSpec.getAttachments(), AttachmentSpec::getKey);
            getKeys(subKeys, CalendarSpec.class, typeSpec.getCalendars(), CalendarSpec::getKey);
            getKeys(subKeys, LinkSpec.class, typeSpec.getLinks(), LinkSpec::getKey);
            getKeys(subKeys, LocationSpec.class, typeSpec.getLocations(), LocationSpec::getKey);
            getKeys(subKeys, RatingSpec.class, typeSpec.getRatings(), RatingSpec::getKey);
            getKeys(subKeys, StateSpec.class, typeSpec.getStates(), StateSpec::getKey);
            getKeys(subKeys, TagSpec.class, typeSpec.getTags(), TagSpec::getKey);
            result.put(typeSpec.getKey(), subKeys);
        }
        return result;
    }

    private static <T> void getKeys(Map<String, Set<String>> subKeys, Class<T> classInstance, List<T> list,
                                    Function<T, String> mapper) {
        if (list != null) {
            subKeys.put(classInstance.getSimpleName(), list.stream().map(mapper).collect(Collectors.toSet()));
        }
    }

    @SneakyThrows
    private void processUniqueFields(Map<String, TypeSpec> types) {
        for (TypeSpec typeSpec: types.values()) {
            if (StringUtils.isBlank(typeSpec.getDataSpec())) {
                continue;
            }

            JsonNode node = JsonLoader.fromString(typeSpec.getDataSpec());
            Set<UniqueFieldSpec> uniqueFields = Sets.newHashSet();
            processNode(node, "$", uniqueFields);
            typeSpec.setUniqueFields(uniqueFields);
        }
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

    private String getTenantKeyValue() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
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
                .collect(Collectors.toMap(TypeSpec::getKey, Function.identity(),
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    }, LinkedHashMap::new));
            return result;
        }
    }

    /**
     * XM Entity Type specifications support inheritance based on key hierarchy
     * with data extension.
     */
    private void inheritance(Map<String, TypeSpec> types, String tenant) {
        List<String> keys = types.keySet().stream()
            .sorted(Comparator.comparingInt(k -> k.split(TYPE_SEPARATOR_REGEXP).length))
            .collect(Collectors.toList());
        for (String key : keys) {
            int level = key.split(TYPE_SEPARATOR_REGEXP).length;
            if (level > 1) {
                TypeSpec type = types.get(key);
                TypeSpec parentType = types.get(StringUtils.substringBeforeLast(key, TYPE_SEPARATOR));
                if (parentType != null) {
                    types.put(key, extend(type, parentType, tenant));
                }
            }
        }
    }

    /**
     * Xm Entity Type specifications data extention from parent Type.
     *
     * @param type       entity Type specification for extention
     * @param parentType parent entity Type specification for cloning
     * @return entity Type specification that extended from parent Type
     */
    private TypeSpec extend(TypeSpec type, TypeSpec parentType, String tenant) {
        type.setIcon(type.getIcon() != null ? type.getIcon() : parentType.getIcon());
        extendDataSpec(type, parentType, tenant);
        extendDataForm(type, parentType, tenant);
        type.setAccess(ignorableUnion(ACCESS, type, parentType));
        type.setAttachments(ignorableUnion(ATTACHMENTS, type, parentType));
        type.setCalendars(ignorableUnion(CALENDARS, type, parentType));
        type.setFunctions(ignorableUnion(FUNCTIONS, type, parentType));
        type.setLinks(ignorableUnion(LINKS, type, parentType));
        type.setLocations(ignorableUnion(LOCATIONS, type, parentType));
        type.setRatings(ignorableUnion(RATINGS, type, parentType));
        type.setStates(ignorableUnion(STATES, type, parentType));
        type.setTags(ignorableUnion(TAGS, type, parentType));
        return type;
    }

    @SneakyThrows
    private void extendDataSpec(TypeSpec type, TypeSpec parentType, String tenant) {
        XmEntityTenantConfig entityTenantConfig = this.tenantConfigService.getXmEntityTenantConfig(tenant);
        Boolean isInheritanceEnabled = entityTenantConfig.getEntitySpec().getEnableDataSpecInheritance();
        if (isFeatureEnabled(isInheritanceEnabled, type.getDataSpecInheritance()) && hasDataSpec(type, parentType)) {
            ObjectMapper objectMapper = new ObjectMapper();
            var target = objectMapper.readValue(type.getDataSpec(), Map.class);
            var parent = objectMapper.readValue(parentType.getDataSpec(), Map.class);
            if (parent.containsKey("additionalProperties")) {
                parent.put("additionalProperties", true);
            }
            target.put(XM_ENTITY_INHERITANCE_DEFINITION, Map.of(parentType.getKey(), parent));
            target.put("$ref", "#/" + XM_ENTITY_INHERITANCE_DEFINITION + "/" + parentType.getKey());
            String mergedJson = objectMapper.writeValueAsString(target);
            type.setDataSpec(mergedJson);
        } else {
            type.setDataSpec(type.getDataSpec() != null ? type.getDataSpec() : parentType.getDataSpec());
        }
    }

    private boolean hasDataSpec(TypeSpec type, TypeSpec parentType) {
        return type.getDataSpec() != null && parentType.getDataSpec() != null;
    }

    private boolean isFeatureEnabled(Boolean tenantFlag, Boolean entityFlag) {
        return TRUE.equals(entityFlag) || (TRUE.equals(tenantFlag) && !FALSE.equals(entityFlag));
    }

    private boolean hasDataForm(TypeSpec type, TypeSpec parentType) {
        return type.getDataForm() != null && parentType.getDataForm() != null;
    }

    @SneakyThrows
    private void extendDataForm(TypeSpec type, TypeSpec parentType, String tenant) {
        XmEntityTenantConfig entityTenantConfig = this.tenantConfigService.getXmEntityTenantConfig(tenant);
        Boolean isInheritanceEnabled = entityTenantConfig.getEntitySpec().getEnableDataFromInheritance();
        if (isFeatureEnabled(isInheritanceEnabled, type.getDataFormInheritance()) && hasDataForm(type, parentType)) {
            ObjectMapper objectMapper = new ObjectMapper();
            var defaults = objectMapper.readValue(parentType.getDataForm(), Map.class);
            ObjectReader updater = objectMapper.readerForUpdating(defaults);
            Map<String, Object> merged = updater.readValue(type.getDataForm());
            String mergedJson = objectMapper.writeValueAsString(merged);
            type.setDataForm(mergedJson);
        } else {
            type.setDataForm(type.getDataForm() != null ? type.getDataForm() : parentType.getDataForm());
        }
    }

    /**
     * Combine type specifications considering ignore list.
     *
     * @param typeSpecParam  represents enum for type specification
     * @param type       entity Type specification for extention
     * @param parentType parent entity Type specification for cloning
     * @return union list or list from child type in case inheritance prohibited
     */
    private static <T> List<T> ignorableUnion(TypeSpecParameter typeSpecParam, TypeSpec type, TypeSpec parentType) {
        List<T> parameters = (List<T>) typeSpecParam.getParameterResolver().apply(type);
        List<T> parentParameters = (List<T>) typeSpecParam.getParameterResolver().apply(parentType);
        return type.getIgnoreInheritanceFor().contains(typeSpecParam.getType()) ?
            parameters : union(parameters, parentParameters);
    }

    private TypeSpec filterFunctions(TypeSpec spec) {
        TypeSpec clone = spec.toBuilder().build();
        return dynamicPermissionCheckService.filterInnerListByPermission(clone,
            clone::getFunctions,
            clone::setFunctions,
            FunctionSpec::getDynamicPrivilegeKey);
    }

    @SneakyThrows
    public String generateJsonSchema() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);

        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper);
        JsonSchema jsonSchema = schemaGen.generateSchema(XmEntitySpec.class);
        rejectAdditionalProperties(jsonSchema);
        StringWriter json = new StringWriter();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.writeValue(json, jsonSchema);
        return json.toString();
    }

    private static void rejectAdditionalProperties(JsonSchema jsonSchema) {
        if (jsonSchema.isObjectSchema()) {
            ObjectSchema objectSchema = jsonSchema.asObjectSchema();
            ObjectSchema.AdditionalProperties additionalProperties = objectSchema.getAdditionalProperties();
            if (additionalProperties instanceof ObjectSchema.SchemaAdditionalProperties) {
                rejectAdditionalProperties(((ObjectSchema.SchemaAdditionalProperties) additionalProperties).getJsonSchema());
            } else {
                for (JsonSchema property : objectSchema.getProperties().values()) {
                    rejectAdditionalProperties(property);
                }
                objectSchema.rejectAdditionalProperties();
            }

            // fix for correct schema validation (i found only string usage, and correct validation required string in this place),
            // but we need keep backward capability
            if ("urn:jsonschema:com:icthh:xm:ms:entity:domain:spec:StateSpec".equals(objectSchema.getId())) {
                objectSchema.getProperties().put("icon", new StringSchema());
                objectSchema.getProperties().put("color", new StringSchema());
            }
        } else if (jsonSchema.isArraySchema()) {
            ArraySchema.Items items = jsonSchema.asArraySchema().getItems();
            if (items.isSingleItems()) {
                rejectAdditionalProperties(items.asSingleItems().getSchema());
            } else if (items.isArrayItems()) {
                for (JsonSchema schema : items.asArrayItems().getJsonSchemas()) {
                    rejectAdditionalProperties(schema);
                }
            }
        }
    }

    public interface EntitySpecUpdateListener {
        void onEntitySpecUpdate(Map<String, TypeSpec> specs, String tenantKey);
    }

}
