package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_METHOD_NOT_SUPPORTED;
import static java.lang.Integer.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.google.common.collect.ImmutableList;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.domain.spec.NextSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class XmEntityGeneratorService {

    private static final String GENERATOR_VERSION = "2.0.2";

    private static final ImmutableList<String> RANDOM_STRINGS = ImmutableList.of(
        "other", "another", "different", "second", "new", "fresh", "any", "whatever"
    );
    private static final int MAX_TAGS_BY_TYPE = 10;
    private static final String LOCATIONS_FOR_GENERATOR_JSON = "locations-for-generator.json";
    private static final String REQUIRED = "required";
    private static final String PROPERTIES = "properties";
    private static final String TYPE = "type";

    private final XmEntityService xmEntityService;
    private final XmEntitySpecService xmEntitySpecService;
    private final String locationsJson;
    private final ObjectMapper mapper;
    private final XmAuthenticationContextHolder authContextHolder;

    public XmEntityGeneratorService(XmEntityService xmEntityService,
                                    XmEntitySpecService xmEntitySpecService,
                                    XmAuthenticationContextHolder authContextHolder,
                                    ObjectMapper mapper) {
        this.xmEntityService = xmEntityService;
        this.xmEntitySpecService = xmEntitySpecService;
        this.locationsJson = getLocationsJson();
        this.authContextHolder = authContextHolder;
        this.mapper = mapper;
    }

    @SneakyThrows
    private TypeSpec getRandomTypeSpec(String rootTypeKey) {
        List<TypeSpec> availableTypeSpecs = xmEntitySpecService.findNonAbstractTypesByPrefix(rootTypeKey);
        return availableTypeSpecs.get(new SecureRandom().nextInt(availableTypeSpecs.size()));
    }

    public XmEntity generateXmEntity(String rootTypeKey) {
        TypeSpec typeSpec = getRandomTypeSpec(rootTypeKey);
        String name = typeSpec.getName().entrySet().iterator().next().getValue();
        if (!Constants.TENANT_TYPE_KEY.equals(rootTypeKey)) {
            name = name.concat(" name");
        } else {
            name = name.concat(String.valueOf(System.currentTimeMillis()));
        }
        XmEntity xmEntity = new XmEntity().key(UUID.randomUUID().toString())
            .typeKey(typeSpec.getKey())
            .stateKey(generateXmEntityState(xmEntitySpecService, typeSpec))
            .name(name)
            .startDate(generateXmEntityStartDate())
            .updateDate(Instant.now())
            .endDate(generateXmEntityEndDate())
            .description(String.format("Generated [%s] Generator version [%s]", typeSpec.getName().entrySet().iterator()
                .next().getValue(), GENERATOR_VERSION))
            .avatarUrl(generateXmEntityAvatarUrl())
            .data(generateXmEntityData(typeSpec))
            .locations(generateLocations(typeSpec.getLocations()))
            .tags(generateTags(typeSpec))
            .createdBy(authContextHolder.getContext().getRequiredUserKey());

        return xmEntityService.save(xmEntity);
    }

    private Set<Location> generateLocations(List<LocationSpec> locationSpecs) {
        if (isEmpty(locationSpecs)) {
            return emptySet();
        }

        Set<Location> result = new HashSet<>();
        for (val locationSpec : locationSpecs) {
            result.addAll(generateLocations(locationSpec));
        }

        return result;
    }

    private Set<Location> generateLocations(LocationSpec locationSpec) {
        Set<Location> result = new HashSet<>();

        List<Location> originalLocationStubs = getLocationStubs();
        val locationStubs = originalLocationStubs.stream().filter(l -> new SecureRandom().nextBoolean())
                                                          .collect(toList());
        if (isEmpty(locationStubs)) {
            int randomLocationPosition = RandomUtils.nextInt(0, originalLocationStubs.size() - 1);
            Location randomLocationStub = originalLocationStubs.get(randomLocationPosition);
            locationStubs.add(randomLocationStub);
        }

        int locationsLimit = min(locationSpec.getMax(), locationStubs.size());
        int countLocations = RandomUtils.nextInt(1, locationsLimit);

        IntStream.range(0, countLocations).forEachOrdered(i -> {
            Location locationStub = locationStubs.get(i);
            updateLocationStubToLocation(locationSpec, locationStub);
            result.add(locationStub);
        });

        return result;
    }

    private void updateLocationStubToLocation(LocationSpec locationSpec, Location locationStub) {

        locationStub.typeKey(locationSpec.getKey()).name(randomAnyString() + " locations");

        if (RandomUtils.nextInt(1, 10) < 5) {
            locationStub.addressLine1(null).addressLine2(null).city(null).region(null);
        } else if (RandomUtils.nextInt(1, 10) > 5) {
            locationStub.latitude(null).longitude(null);
        }
        // TODO: change this when Country DB entity will be removed
        locationStub.setCountryKey(null);
    }

    private Set<Tag> generateTags(TypeSpec typeSpec) {
        Set<Tag> tags = new HashSet<>();
        for (TagSpec tagSpec: randomCollectionElement(typeSpec.getTags())) {
            int countTags = RandomUtils.nextInt(1, MAX_TAGS_BY_TYPE);
            IntStream.range(0, countTags).forEach(i -> tags.add(createTag(tagSpec, i + 1)));
            log.debug("Generate {} tags for type {}", countTags, tagSpec.getKey());
        }
        return tags;
    }

    private Tag createTag(TagSpec tagSpec, int nameIndex) {
        return new Tag().typeKey(tagSpec.getKey()).name(randomAnyString() + " tag " + nameIndex)
            .startDate(Instant.now());
    }

    @SneakyThrows
    private List<Location> getLocationStubs() {
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, Location.class);
        return mapper.readValue(this.locationsJson, type);
    }

    @SneakyThrows
    private static String getLocationsJson() {
        InputStream inputStream = new ClassPathResource(LOCATIONS_FOR_GENERATOR_JSON).getInputStream();
        return IOUtils.toString(inputStream, UTF_8);
    }

    @SneakyThrows
    private static String generateXmEntityState(XmEntitySpecService xmEntitySpecService, TypeSpec typeSpec) {
        List<NextSpec> next = xmEntitySpecService.next(typeSpec.getKey(), null);
        return next.isEmpty() ? null : next.get(new SecureRandom().nextInt(next.size())).getStateKey();
    }

    private static Instant generateXmEntityStartDate() {
        return Instant.now();
    }

    private static Instant generateXmEntityEndDate() {
        return null;
    }

    private static String generateXmEntityAvatarUrl() {
        return "";
    }

    @SneakyThrows
    private Map<String, Object> generateXmEntityData(TypeSpec typeSpec) {

        if (isEmpty(typeSpec.getDataSpec())) {
            return emptyMap();
        }

        val type = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> dataSpec = mapper.readValue(typeSpec.getDataSpec(), type);

        return generateXmEntityData(dataSpec);
    }

    private Map<String, Object> generateXmEntityData(Map<String, Object> dataSpec) {
        Map<String, Object> data = new HashMap<>();

        Collection<String> requiredFields = (Collection<String>) dataSpec.get(REQUIRED);
        Map<String, Object> properties = (Map<String, Object>) dataSpec.get(PROPERTIES);

        if (requiredFields == null) {
            return data;
        }

        for (val requiredField : requiredFields) {
            Map<String, Object> fieldSpec = (Map<String, Object>) properties.get(requiredField);
            String fieldType = String.valueOf(fieldSpec.get(TYPE));
            data.put(requiredField, randomDataValue(fieldType, fieldSpec));
        }

        return data;
    }

    @SneakyThrows
    private Object randomDataValue(String type, Map<String, Object> fieldSpec) {
        Object fieldValue = null;
        switch (type) {
            case "string":
                fieldValue = randomAnyString();
                break;
            case "number":
                fieldValue = new SecureRandom().nextInt();
                break;
            case "boolean":
                fieldValue = new SecureRandom().nextBoolean();
                break;
            case "object":
                fieldValue = generateXmEntityData(fieldSpec);
                break;
            default: throw new BusinessException(ERR_METHOD_NOT_SUPPORTED, "Generator not support of data type " + type);
        }
        return fieldValue;
    }

    private static String randomAnyString() {
        return RANDOM_STRINGS.get(RandomUtils.nextInt(0, RANDOM_STRINGS.size() - 1));
    }

    @SneakyThrows
    private static <T> Collection<T> randomCollectionElement(Collection<T> inCollection) {
        if (isEmpty(inCollection)) {
            return emptySet();
        }

        Set<Integer> tagPositions = new HashSet<>();
        IntStream.range(0, inCollection.size() - 1).filter(t -> new SecureRandom().nextBoolean())
                                                   .forEach(tagPositions::add);
        tagPositions.add(RandomUtils.nextInt(0, inCollection.size() - 1));

        log.debug("get tags in positions {}", tagPositions);

        List<T> inList = new ArrayList<>(inCollection);
        return tagPositions.stream().map(inList::get).collect(toList());
    }

}
