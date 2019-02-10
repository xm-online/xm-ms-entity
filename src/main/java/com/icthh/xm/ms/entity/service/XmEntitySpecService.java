package com.icthh.xm.ms.entity.service;

import static com.github.fge.jackson.NodeType.OBJECT;
import static com.github.fge.jackson.NodeType.getNodeType;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.config.client.repository.TenantConfigRepository;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.spec.AttachmentSpec;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
    private static final String CONTEXT_NAME = "contextName";
    private static final String DEFAULT = "DEFAULT";

    private final AntPathMatcher matcher = new AntPathMatcher();

    private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final ConcurrentHashMap<String, Map<String, TypeSpec>> types = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Map<String, List<TypeSpec>>> originalContextSpecs = new ConcurrentHashMap<>();

    private final TenantConfigRepository tenantConfigRepository;

    private final ApplicationProperties applicationProperties;

    private final TenantContextHolder tenantContextHolder;

    private final EntityCustomPrivilegeService entityCustomPrivilegeService;


    private String getTenantKeyValue() {
        return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
    }

    @SneakyThrows
    public void updateXmEntitySpec(String xmEntitySpecString) {
        String configName = applicationProperties.getSpecificationName();

        // Simple validation correct structure.
        XmEntitySpec xmEntitySpec = mapper.readValue(xmEntitySpecString, XmEntitySpec.class);

        tenantConfigRepository.updateConfig(getTenantKeyValue(), "/" + configName, xmEntitySpecString);
    }

    protected Map<String, TypeSpec> getTypeSpecs() {
        String tenantKeyValue = getTenantKeyValue();
        if (!types.containsKey(tenantKeyValue)) {
            throw new IllegalArgumentException("Tenant configuration not found");
        }
        return types.get(tenantKeyValue);
    }

    private Map<String, TypeSpec> toTypeSpecsMap(Collection<TypeSpec> typeSpecs) {
        if (isEmpty(typeSpecs)) {
            return Collections.emptyMap();
        } else {
            // Convert List<TypeSpec> to Map<key, TypeSpec>
            Map<String, TypeSpec> result = typeSpecs.stream()
                .collect(Collectors.toMap(TypeSpec::getKey, Function.identity(),
                    (u, v) -> {
                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                    }, LinkedHashMap::new));

            // add inheritance
            inheritance(result);
            processUniqueFields(result);

            return result;
        }
    }

    /**
     * XM Entity Type specifications support inheritance based on key hierarchy
     * with data extension.
     */
    private void inheritance(Map<String, TypeSpec> types) {
        List<String> keys = types.keySet().stream()
            .sorted(Comparator.comparingInt(k -> k.split(TYPE_SEPARATOR_REGEXP).length))
            .collect(Collectors.toList());
        for (String key : keys) {
            int level = key.split(TYPE_SEPARATOR_REGEXP).length;
            if (level > 1) {
                TypeSpec type = types.get(key);
                TypeSpec parentType = types.get(StringUtils.substringBeforeLast(key, TYPE_SEPARATOR));
                if (parentType != null) {
                    types.put(key, extend(type, parentType));
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
    private static TypeSpec extend(TypeSpec type, TypeSpec parentType) {
        type.setIcon(type.getIcon() != null ? type.getIcon() : parentType.getIcon());
        type.setDataSpec(type.getDataSpec() != null ? type.getDataSpec() : parentType.getDataSpec());
        type.setDataForm(type.getDataForm() != null ? type.getDataForm() : parentType.getDataForm());
        type.setAccess(XmEntitySpecService.union(type.getAccess(), parentType.getAccess()));
        type.setAttachments(XmEntitySpecService.union(type.getAttachments(), parentType.getAttachments()));
        type.setCalendars(XmEntitySpecService.union(type.getCalendars(), parentType.getCalendars()));
        type.setFunctions(XmEntitySpecService.union(type.getFunctions(), parentType.getFunctions()));
        type.setLinks(XmEntitySpecService.union(type.getLinks(), parentType.getLinks()));
        type.setLocations(XmEntitySpecService.union(type.getLocations(), parentType.getLocations()));
        type.setRatings(XmEntitySpecService.union(type.getRatings(), parentType.getRatings()));
        type.setStates(XmEntitySpecService.union(type.getStates(), parentType.getStates()));
        type.setTags(XmEntitySpecService.union(type.getTags(), parentType.getTags()));
        return type;
    }

    /**
     * Union for nullable lists.
     *
     * @param list1 first list
     * @param list2 second list
     * @return concatenated list
     */
    private static <E> List<E> union(final List<E> list1, final List<E> list2) {
        final List<E> result = new ArrayList<>();
        if (list1 != null) {
            result.addAll(list1);
        }
        if (list2 != null) {
            result.addAll(list2);
        }
        return result;
    }

    /**
     * Search entity Type specification by key.
     *
     * @param key entity Type specification key
     * @return instance of entity Type specification if present
     */
    @IgnoreLogginAspect
    public TypeSpec findTypeByKey(String key) {
        return getTypeSpecs().get(key);
    }

    /**
     * Search of all entity Type specifications.
     *
     * @return list of entity Types specifications
     */
    public List<TypeSpec> findAllTypes() {
        return new ArrayList<>(getTypeSpecs().values());
    }

    /**
     * Search of all entity Type specifications that marked as application.
     *
     * @return list of entity Types specifications that defines as application.
     */
    public List<TypeSpec> findAllAppTypes() {
        return findAllTypes().stream().filter(TypeSpec::getIsApp).collect(Collectors.toList());
    }

    /**
     * Search of all not an abstract entity Type specifications.
     *
     * @return list of entity Types specifications that not an abstract.
     */
    public List<TypeSpec> findAllNonAbstractTypes() {
        return findAllTypes().stream().filter(t -> !t.getIsAbstract()).collect(Collectors.toList());
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
        List<TypeSpec> allNonAbstractTypes = findAllNonAbstractTypes();
        if (isEmpty(prefix)) {
            return allNonAbstractTypes;
        } else {
            return allNonAbstractTypes.stream().filter(bySpecTypesPrefix(prefix)).collect(Collectors.toList());
        }
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
    public Optional<StateSpec> findState(String key, String stateKey) {
        return getTypeSpecs().get(key).getStates().stream().filter(s -> s.getKey().equals(stateKey)).findFirst();
    }

    private <T> Iterable<T> nullSafe(Iterable<T> itr) {
        return itr == null ? emptyList() : itr;
    }

    private <T> Stream<T> streamOf(List<T> list) {
        return list == null ? Stream.empty() : list.stream();
    }

    @IgnoreLogginAspect
    public Optional<FunctionSpec> findFunction(String functionKey) {
        for (TypeSpec ts : getTypeSpecs().values()) {
            for (FunctionSpec fs : nullSafe(ts.getFunctions())) {
                if (fs.getKey().equals(functionKey)) {
                    return Optional.of(fs);
                }
            }
        }
        return Optional.empty();
    }

    @IgnoreLogginAspect
    public Optional<FunctionSpec> findFunction(String typeKey, String functionKey) {
        TypeSpec typeSpec = getTypeSpecs().get(typeKey);
        if (typeSpec == null) {
            return Optional.empty();
        }
        Stream<FunctionSpec> functionSpecStream = streamOf(typeSpec.getFunctions());
        return functionSpecStream.filter(fs -> fs.getKey().equals(functionKey)).findFirst();
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
        Map<String, Map<String, Set<String>>> result = new HashMap<>();
        for (TypeSpec typeSpec : findAllTypes()) {
            Map<String, Set<String>> subKeys = new HashMap<>();
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
            if (isBlank(typeSpec.getDataSpec())) {
                continue;
            }

            JsonNode node = JsonLoader.fromString(typeSpec.getDataSpec());
            Set<UniqueFieldSpec> uniqueFields = new HashSet<>();
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

    @Override
    @SneakyThrows
    @IgnoreLogginAspect
    public void onRefresh(String updatedKey, String config) {
        String specPathPattern = applicationProperties.getSpecificationPathPattern();
        String contextName = DEFAULT;
        try {
            if (isMultifileFileSpec(updatedKey)) {
                specPathPattern = applicationProperties.getMultifilesSpecificationPathPattern();
                contextName = matcher.extractUriTemplateVariables(specPathPattern, updatedKey).get(CONTEXT_NAME);
            }

            String tenant = matcher.extractUriTemplateVariables(specPathPattern, updatedKey).get(TENANT_NAME);
            originalContextSpecs.putIfAbsent(tenant, new ConcurrentHashMap<>());
            if (org.apache.commons.lang3.StringUtils.isBlank(config)) {
                originalContextSpecs.get(tenant).remove(contextName);
                if (originalContextSpecs.get(tenant).isEmpty()) {
                    types.remove(tenant);
                    return;
                }
            }

            Set<TypeSpec> typeSpecs = new TreeSet<>(comparing(TypeSpec::getKey));
            originalContextSpecs.get(tenant).values().forEach(typeSpecs::addAll);

            if (org.apache.commons.lang3.StringUtils.isNotBlank(config)) {
                XmEntitySpec spec = mapper.readValue(config, XmEntitySpec.class);
                typeSpecs.addAll(spec.getTypes());
                originalContextSpecs.get(tenant).put(contextName, spec.getTypes());
            }


            Map<String, TypeSpec> value = toTypeSpecsMap(typeSpecs);
            types.put(tenant, value);
            entityCustomPrivilegeService.updateApplicationPermission(value, tenant);
            log.info("Specification was for tenant {} updated", tenant);
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return isOneFileSpec(updatedKey) || isMultifileFileSpec(updatedKey);
    }

    private boolean isOneFileSpec(String updatedKey) {
        String specificationPathPattern = applicationProperties.getSpecificationPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    private boolean isMultifileFileSpec(String updatedKey) {
        String specificationPathPattern = applicationProperties.getMultifilesSpecificationPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }

    @Nullable
    public String findFirstStateForTypeKey(String typeKey) {
        return ofNullable(findTypeByKey(typeKey))
            .map(TypeSpec::getStates)
            .filter(CollectionUtils::isNotEmpty)
            .map(it -> it.get(0))
            .map(StateSpec::getKey)
            .orElse(null);
    }

}
