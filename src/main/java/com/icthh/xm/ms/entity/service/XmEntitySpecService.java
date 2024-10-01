package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Maps;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
import com.icthh.xm.ms.entity.domain.spec.DataSchema;
import com.icthh.xm.ms.entity.domain.spec.DefinitionSpec;
import com.icthh.xm.ms.entity.domain.spec.EventSpec;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.RatingSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import com.networknt.schema.JsonSchema;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getFirst;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * XM Entity Specification service, that provides extra possibilities to
 * manipulate with pure model.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntitySpecService implements RefreshableConfiguration {

    private static final String TYPE_SEPARATOR = ".";
    private static final String TENANT_NAME = "tenantName";
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final TenantConfigRepository tenantConfigRepository;
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;
    private final XmEntitySpecContextService xmEntitySpecContextService;
    private final List<EntitySpecUpdateListener> entitySpecUpdateListeners;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;

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
        return t -> Boolean.TRUE.equals(t.getIsApp());
    }

    public static Predicate<TypeSpec> isNotAbstract() {
        return t -> !Boolean.TRUE.equals(t.getIsAbstract());
    }

    @Override
    @SneakyThrows
    @IgnoreLogginAspect
    public void onRefresh(String updatedKey, String config) {

        try {
            String tenant = extractTenantName(updatedKey);
            xmEntitySpecContextService.updateByFileState(updatedKey, config, tenant);
            xmEntitySpecContextService.updateByTenantState(tenant);
            log.info("Specification was for tenant {} updated from file {}", tenant, updatedKey);
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        Set<String> tenants = paths.stream().map(this::extractTenantName).collect(Collectors.toSet());
        tenants.forEach(tenantKey -> {
            Map<String, TypeSpec> tenantEntitySpec = xmEntitySpecContextService.typesByTenant(tenantKey);
            entitySpecUpdateListeners.forEach(it -> it.onEntitySpecUpdate(tenantEntitySpec, tenantKey));
        });
    }

    public Collection<TypeSpec> getAllSpecs() {
        return xmEntitySpecContextService.typesByTenant(getTenantKeyValue()).values();
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
        mapper.readValue(xmEntitySpecString, XmEntitySpec.class);

        tenantConfigRepository.updateConfig(getTenantKeyValue(), "/" + configName, xmEntitySpecString);
    }

    /**
     * Assumption value of types.get(tenantKey) could be null
     * @return map with nullSafe check
     */
    protected Map<String, TypeSpec> getTypeSpecs() {
        String tenantKeyValue = getTenantKeyValue();
        return xmEntitySpecContextService.typesByTenantStrict(tenantKeyValue);
    }

    public List<DefinitionSpec> getDefinitions() {
        String tenantKeyValue = getTenantKeyValue();
        return xmEntitySpecContextService.getDefinitions(tenantKeyValue);
    }

    public List<DataSchema> getDataSchemas() {
        String tenantKeyValue = getTenantKeyValue();
        List<DefinitionSpec> definitions = xmEntitySpecContextService.getDefinitions(tenantKeyValue);
        Collection<TypeSpec> types = getAllSpecs();
        List<DataSchema> dataSchemas = new ArrayList<>();
        definitions.forEach(it -> dataSchemas.add(new DataSchema(it.getKey(), it.getValue(), "definition")));
        types.forEach(it -> dataSchemas.add(new DataSchema(it.getKey(), it.getDataSpec(), "entity")));
        return dataSchemas;
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<TypeSpec> getTypeSpecByKey(String key) {
        return ofNullable(getTypeSpecs().get(key)).map(this::filterFunctions);
    }

    @LoggingAspectConfig(resultDetails = false)
    public Optional<JsonSchema> getDataJsonSchemaByKey(String key) {
        return ofNullable(xmEntitySpecContextService.dataSpecJsonSchemas(getTenantKeyValue())).map(it -> it.get(key));
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
    public Optional<FunctionSpec> findFunction(String functionKey, String httpMethod) {
        String tenantKey = getTenantKeyValue();
        Map<String, FunctionSpec> functions = xmEntitySpecContextService.functionsByTenant(tenantKey);
        return Optional.of(functions)
            .map(fs -> fs.get(functionKey))
            .filter(fs -> filterAndLogByHttpMethod(httpMethod, fs))
                .or(() -> xmEntitySpecContextService.functionsByTenantOrdered(tenantKey).stream()
                        .filter(fs -> fs.getPath() != null)
                        .filter(fs -> matcher.match(fs.getPath(), functionKey))
                        .filter(fs -> filterByHttpMethod(httpMethod, fs))
                        .findFirst());

    }

    private static boolean filterAndLogByHttpMethod(String httpMethod, FunctionSpec fs) {
        if (filterByHttpMethod(httpMethod, fs)) {
            return true;
        } else {
            log.error("Function {} not found for http method {}", fs.getKey(), httpMethod);
            return false;
        }
    }

    private static boolean filterByHttpMethod(String httpMethod, FunctionSpec fs) {
        return isEmpty(fs.getHttpMethods()) || fs.getHttpMethods().contains(httpMethod);
    }

    @IgnoreLogginAspect
    public Optional<FunctionSpec> findEntityFunction(String typeKey, String functionKey) {
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
        return next(key, stateKey).stream()
            .flatMap(n -> findState(key, n.getStateKey()).stream())
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

    private String getTenantKeyValue() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    private TypeSpec filterFunctions(TypeSpec spec) {
        TypeSpec clone = spec.toBuilder().build();
        return dynamicPermissionCheckService.filterInnerListByPermission(clone,
            clone::getFunctions,
            clone::setFunctions,
            FunctionSpec::getDynamicPrivilegeKey);
    }

    public interface EntitySpecUpdateListener {
        void onEntitySpecUpdate(Map<String, TypeSpec> specs, String tenantKey);
    }

}
