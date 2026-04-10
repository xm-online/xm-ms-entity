package com.icthh.xm.ms.entity.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for XmEntity JSON serialization/deserialization through DTO layer.
 *
 * Tests the full pipeline:
 * - Outbound: XmEntity -> XmEntityMapper.toDto() -> ObjectMapper -> JSON
 * - Inbound:  JSON -> ObjectMapper -> XmEntityDto -> XmEntityMapper.toEntity() -> XmEntity
 *
 * Verifies backward compatibility after migration to DTO + facade pattern.
 */
@Transactional
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityDtoJsonSerializationIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private XmEntityMapper xmEntityMapper;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private EntityManager em;

    private static final Instant START = Instant.parse("2024-01-15T10:30:00Z");
    private static final Instant UPDATE = Instant.parse("2024-01-15T12:00:00Z");
    private static final Instant END = Instant.parse("2024-12-31T23:59:59Z");

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    // ==================== Helpers ====================

    private XmEntity createMinimalEntity() {
        XmEntity entity = new XmEntity();
        entity.setId(100L);
        entity.setTypeKey("ACCOUNT");
        entity.setKey("account-1");
        entity.setName("Test Account");
        entity.setStartDate(START);
        entity.setUpdateDate(UPDATE);
        return entity;
    }

    private XmEntity createFullEntity() {
        XmEntity entity = createMinimalEntity();
        entity.setStateKey("ACTIVE");
        entity.setEndDate(END);
        entity.setAvatarUrl("http://example.com/avatar.png");
        entity.setDescription("A test entity description");
        entity.setRemoved(true);
        entity.setCreatedBy("admin");
        entity.setUpdatedBy("editor");
        entity.setVersion(5);
        Map<String, Object> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", 42);
        entity.setData(data);
        return entity;
    }

    /** Serialize: XmEntity -> mapper.toDto() -> JSON */
    private JsonNode entityToJson(XmEntity entity) throws Exception {
        XmEntityDto dto = xmEntityMapper.toDto(entity);
        String json = objectMapper.writeValueAsString(dto);
        return objectMapper.readTree(json);
    }

    /** Deserialize: JSON -> XmEntityDto -> mapper.toEntity() */
    private XmEntity jsonToEntity(String json) throws Exception {
        XmEntityDto dto = objectMapper.readValue(json, XmEntityDto.class);
        return xmEntityMapper.toEntity(dto);
    }

    private long persistEntityIdCounter = 90000L;

    private XmEntity persistEntity(String typeKey, String key, String name) {
        long id = persistEntityIdCounter++;
        em.createNativeQuery(
                "INSERT INTO xm_entity (id, type_key, jhi_key, name, start_date, update_date) " +
                "VALUES (:id, :typeKey, :key, :name, :startDate, :updateDate)")
            .setParameter("id", id)
            .setParameter("typeKey", typeKey)
            .setParameter("key", key)
            .setParameter("name", name)
            .setParameter("startDate", java.sql.Timestamp.from(START))
            .setParameter("updateDate", java.sql.Timestamp.from(UPDATE))
            .executeUpdate();
        em.flush();
        em.clear();
        return em.find(XmEntity.class, id);
    }

    private void assertNullOrAbsent(JsonNode json, String field) {
        JsonNode node = json.path(field);
        assertThat(node.isMissingNode() || node.isNull())
            .as("Expected '%s' to be absent or null, but was: %s", field, node)
            .isTrue();
    }

    private Tag createTag(String typeKey, String name, XmEntity owner) {
        Tag tag = new Tag();
        tag.setTypeKey(typeKey);
        tag.setName(name);
        tag.setStartDate(START);
        tag.setXmEntity(owner);
        return tag;
    }

    private Comment createComment(String message, XmEntity owner) {
        Comment comment = new Comment();
        comment.setMessage(message);
        comment.setUserKey("user1");
        comment.setEntryDate(START);
        comment.setXmEntity(owner);
        return comment;
    }

    private Link createLink(String typeKey, XmEntity source, XmEntity target) {
        Link link = new Link();
        link.setId(50L);
        link.setTypeKey(typeKey);
        link.setName("Test Link");
        link.setDescription("Link description");
        link.setStartDate(START);
        link.setEndDate(END);
        link.setSource(source);
        link.setTarget(target);
        return link;
    }

    // ==================== GROUP 1: Serialization - Scalar Fields ====================

    @Test
    void serialize_minimalEntity_emptyCollectionsPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setData(new HashMap<>());

        JsonNode json = entityToJson(entity);

        assertThat(json.get("id").asLong()).isEqualTo(100L);
        assertThat(json.get("typeKey").asText()).isEqualTo("ACCOUNT");
        assertThat(json.get("key").asText()).isEqualTo("account-1");
        assertThat(json.get("name").asText()).isEqualTo("Test Account");
        assertThat(json.has("startDate")).isTrue();
        assertThat(json.has("updateDate")).isTrue();

        // Empty collections present as empty arrays
        assertThat(json.get("tags").isArray()).isTrue();
        assertThat(json.get("tags").size()).isEqualTo(0);
        assertThat(json.get("locations").isArray()).isTrue();
        assertThat(json.get("locations").size()).isEqualTo(0);
        assertThat(json.get("attachments").isArray()).isTrue();
        assertThat(json.get("comments").isArray()).isTrue();
        assertThat(json.get("ratings").isArray()).isTrue();
        assertThat(json.get("calendars").isArray()).isTrue();
        assertThat(json.get("functionContexts").isArray()).isTrue();
        assertThat(json.get("targets").isArray()).isTrue();

        // Empty data map present
        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").size()).isEqualTo(0);

        // Null fields absent or null
        assertNullOrAbsent(json, "endDate");
        assertNullOrAbsent(json, "stateKey");
        assertNullOrAbsent(json, "description");

        // In DTO: sources is @JsonIgnore on getter without field @JsonSerialize -> truly excluded
        assertThat(json.has("sources")).isFalse();
    }

    @Test
    void serialize_allScalarFieldsPopulated() throws Exception {
        XmEntity entity = createFullEntity();

        JsonNode json = entityToJson(entity);

        assertThat(json.get("id").asLong()).isEqualTo(100L);
        assertThat(json.get("typeKey").asText()).isEqualTo("ACCOUNT");
        assertThat(json.get("key").asText()).isEqualTo("account-1");
        assertThat(json.get("name").asText()).isEqualTo("Test Account");
        assertThat(json.get("stateKey").asText()).isEqualTo("ACTIVE");
        assertThat(json.get("description").asText()).isEqualTo("A test entity description");
        assertThat(json.get("avatarUrl").asText()).isEqualTo("http://example.com/avatar.png");
        assertThat(json.get("removed").asBoolean()).isTrue();
        assertThat(json.get("createdBy").asText()).isEqualTo("admin");
        assertThat(json.get("updatedBy").asText()).isEqualTo("editor");
        assertThat(json.get("version").asInt()).isEqualTo(5);
        assertThat(json.has("startDate")).isTrue();
        assertThat(json.has("updateDate")).isTrue();
        assertThat(json.has("endDate")).isTrue();
        assertThat(json.get("data").get("field1").asText()).isEqualTo("value1");
        assertThat(json.get("data").get("field2").asInt()).isEqualTo(42);
    }

    @Test
    void serialize_datesAsIsoStrings() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setEndDate(END);

        JsonNode json = entityToJson(entity);

        assertThat(json.get("startDate").asText()).isEqualTo("2024-01-15T10:30:00Z");
        assertThat(json.get("updateDate").asText()).isEqualTo("2024-01-15T12:00:00Z");
        assertThat(json.get("endDate").asText()).isEqualTo("2024-12-31T23:59:59Z");
        assertThat(json.get("startDate").isTextual()).isTrue();
    }

    @Test
    void serialize_nullOptionalFieldsExcluded() throws Exception {
        XmEntity entity = createMinimalEntity();

        JsonNode json = entityToJson(entity);

        assertNullOrAbsent(json, "endDate");
        assertNullOrAbsent(json, "stateKey");
        assertNullOrAbsent(json, "description");
        assertNullOrAbsent(json, "avatarUrl");
        assertNullOrAbsent(json, "createdBy");
        assertNullOrAbsent(json, "updatedBy");
        assertNullOrAbsent(json, "removed");
    }

    @Test
    void serialize_avatarUrlFieldNaming() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setAvatarUrl("http://example.com/avatar.png");

        JsonNode json = entityToJson(entity);

        assertThat(json.has("avatarUrl")).isTrue();
        assertThat(json.get("avatarUrl").asText()).isEqualTo("http://example.com/avatar.png");
        assertThat(json.has("avatarUrlRelative")).isFalse();
        assertThat(json.has("avatarUrlFull")).isFalse();
    }

    @Test
    void serialize_removedFalseIsPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setRemoved(false);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("removed")).isTrue();
        assertThat(json.get("removed").asBoolean()).isFalse();
    }

    @Test
    void serialize_removedTrueIncluded() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setRemoved(true);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("removed")).isTrue();
        assertThat(json.get("removed").asBoolean()).isTrue();
    }

    /**
     * XmEntityDto doesn't implement Persistable, so no isNew() method.
     * "new" field should not appear.
     */
    @Test
    void serialize_noNewFieldInDto() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setId(null);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("new")).isFalse();
    }

    // ==================== GROUP 2: Serialization - Collections & @JsonIgnore ====================

    /**
     * In DTO: @JsonIgnore on getSources() without field-level @JsonSerialize
     * -> sources truly excluded from JSON.
     */
    @Test
    void serialize_sourcesTrulyExcludedInDto() throws Exception {
        XmEntity entity = createMinimalEntity();
        XmEntity sourceEntity = createMinimalEntity();
        sourceEntity.setId(200L);

        Link link = createLink("LINK.TYPE", sourceEntity, entity);
        entity.setSources(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(entity);

        // In DTO: sources is truly excluded (unlike entity where field @JsonSerialize overrides)
        assertThat(json.has("sources")).isFalse();
    }

    @Test
    void serialize_votesIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();
        Vote vote = new Vote();
        vote.setId(1L);
        vote.setUserKey("user1");
        vote.setValue(5.0);
        vote.setEntryDate(START);
        entity.addVotes(vote);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("votes")).isFalse();
    }

    @Test
    void serialize_eventsIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();
        Event event = new Event();
        event.setId(1L);
        event.setTypeKey("EVENT.TYPE");
        event.setDescription("test event");
        event.setStartDate(START);
        entity.addEvent(event);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("events")).isFalse();
    }

    /**
     * uniqueFields is @JsonIgnore on XmEntity and ignored in mapper.
     */
    @Test
    void serialize_uniqueFieldsIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();
        UniqueField uf = new UniqueField();
        uf.setId(1L);
        entity.setUniqueFields(new HashSet<>(Set.of(uf)));

        JsonNode json = entityToJson(entity);

        assertThat(json.has("uniqueFields")).isFalse();
    }

    @Test
    void serialize_tagsWithXmEntityAsId() throws Exception {
        XmEntity entity = createMinimalEntity();
        Tag tag = createTag("TAG.VIP", "VIP Customer", entity);
        tag.setId(10L);
        entity.setTags(new HashSet<>(Set.of(tag)));

        JsonNode json = entityToJson(entity);

        assertThat(json.has("tags")).isTrue();
        JsonNode tagNode = json.get("tags").get(0);
        assertThat(tagNode.get("id").asLong()).isEqualTo(10L);
        assertThat(tagNode.get("typeKey").asText()).isEqualTo("TAG.VIP");
        assertThat(tagNode.get("name").asText()).isEqualTo("VIP Customer");
        // xmEntity mapped as shallow DTO (only ID) -> serialized as ID via @JsonIdentityReference
        assertThat(tagNode.get("xmEntity").isNumber()).isTrue();
        assertThat(tagNode.get("xmEntity").asLong()).isEqualTo(100L);
    }

    @Test
    void serialize_commentsWithXmEntityAsId() throws Exception {
        XmEntity entity = createMinimalEntity();
        Comment comment = createComment("Hello world", entity);
        comment.setId(20L);
        comment.setClientId("mobile-app");
        comment.setDisplayName("Admin User");
        entity.setComments(new HashSet<>(Set.of(comment)));

        JsonNode json = entityToJson(entity);

        assertThat(json.has("comments")).isTrue();
        JsonNode commentNode = json.get("comments").get(0);
        assertThat(commentNode.get("id").asLong()).isEqualTo(20L);
        assertThat(commentNode.get("message").asText()).isEqualTo("Hello world");
        assertThat(commentNode.get("userKey").asText()).isEqualTo("user1");
        assertThat(commentNode.get("clientId").asText()).isEqualTo("mobile-app");
        assertThat(commentNode.get("displayName").asText()).isEqualTo("Admin User");
        assertThat(commentNode.get("xmEntity").isNumber()).isTrue();
        assertThat(commentNode.get("xmEntity").asLong()).isEqualTo(100L);
        assertThat(commentNode.has("replies")).isFalse();
    }

    @Test
    void serialize_allNonIgnoredCollections() throws Exception {
        XmEntity entity = createMinimalEntity();

        Location loc = new Location();
        loc.setId(1L);
        loc.setTypeKey("LOC.ADDRESS");
        loc.setName("Home");
        loc.setLatitude(48.8566);
        loc.setLongitude(2.3522);
        loc.setCity("Paris");
        loc.setCountryKey("FR");
        loc.setXmEntity(entity);
        entity.setLocations(new HashSet<>(Set.of(loc)));

        Attachment att = new Attachment();
        att.setId(2L);
        att.setTypeKey("ATT.DOCUMENT");
        att.setName("contract.pdf");
        att.setContentUrl("http://files.example.com/contract.pdf");
        att.setValueContentType("application/pdf");
        att.setValueContentSize(1024L);
        att.setStartDate(START);
        att.setXmEntity(entity);
        entity.setAttachments(new HashSet<>(Set.of(att)));

        Rating rating = new Rating();
        rating.setId(3L);
        rating.setTypeKey("RATING.STARS");
        rating.setValue(4.5);
        rating.setStartDate(START);
        rating.setXmEntity(entity);
        entity.setRatings(new HashSet<>(Set.of(rating)));

        Calendar cal = new Calendar();
        cal.setId(4L);
        cal.setTypeKey("CAL.WORK");
        cal.setName("Work Calendar");
        cal.setStartDate(START);
        cal.setTimeZoneId("Europe/Paris");
        cal.setXmEntity(entity);
        entity.setCalendars(new HashSet<>(Set.of(cal)));

        FunctionContext fc = new FunctionContext();
        fc.setId(5L);
        fc.setKey("fc-1");
        fc.setTypeKey("FC.RESULT");
        fc.setStartDate(START);
        Map<String, Object> fcData = new HashMap<>();
        fcData.put("result", "success");
        fc.setData(fcData);
        fc.setXmEntity(entity);
        entity.setFunctionContexts(new HashSet<>(Set.of(fc)));

        JsonNode json = entityToJson(entity);

        assertThat(json.has("locations")).isTrue();
        JsonNode locNode = json.get("locations").get(0);
        assertThat(locNode.get("typeKey").asText()).isEqualTo("LOC.ADDRESS");
        assertThat(locNode.get("city").asText()).isEqualTo("Paris");
        assertThat(locNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("attachments")).isTrue();
        JsonNode attNode = json.get("attachments").get(0);
        assertThat(attNode.get("name").asText()).isEqualTo("contract.pdf");
        assertThat(attNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("ratings")).isTrue();
        JsonNode ratingNode = json.get("ratings").get(0);
        assertThat(ratingNode.get("value").asDouble()).isEqualTo(4.5);
        assertThat(ratingNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("calendars")).isTrue();
        JsonNode calNode = json.get("calendars").get(0);
        assertThat(calNode.get("name").asText()).isEqualTo("Work Calendar");
        assertThat(calNode.get("timeZoneId").asText()).isEqualTo("Europe/Paris");
        assertThat(calNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("functionContexts")).isTrue();
        JsonNode fcNode = json.get("functionContexts").get(0);
        assertThat(fcNode.get("key").asText()).isEqualTo("fc-1");
        assertThat(fcNode.get("data").get("result").asText()).isEqualTo("success");
        assertThat(fcNode.get("xmEntity").isNumber()).isTrue();
    }

    @Test
    void serialize_multipleCollectionsTogether() throws Exception {
        XmEntity entity = createMinimalEntity();

        Tag tag1 = createTag("TAG.A", "Alpha", entity);
        tag1.setId(1L);
        Tag tag2 = createTag("TAG.B", "Beta", entity);
        tag2.setId(2L);
        entity.setTags(new HashSet<>(Set.of(tag1, tag2)));

        Location loc = new Location();
        loc.setId(3L);
        loc.setTypeKey("LOC.HOME");
        loc.setName("Home");
        loc.setXmEntity(entity);
        entity.setLocations(new HashSet<>(Set.of(loc)));

        Comment comment = createComment("Nice!", entity);
        comment.setId(4L);
        entity.setComments(new HashSet<>(Set.of(comment)));

        JsonNode json = entityToJson(entity);

        assertThat(json.get("tags").size()).isEqualTo(2);
        assertThat(json.get("locations").size()).isEqualTo(1);
        assertThat(json.get("comments").size()).isEqualTo(1);
    }

    // ==================== GROUP 3: Serialization - Links (via DTO, not SimpleLinkSerializer) ====================

    /**
     * In DTO: targets serialized as regular LinkDto objects (not SimpleLinkSerializer).
     * LinkMapper.targetXmEntityToDto maps the same subset of fields as SimpleLinkSerializer.
     */
    @Test
    void serialize_targetsViaDto() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(200L);
        target.setTypeKey("PRODUCT");
        target.setKey("product-1");
        target.setName("Test Product");
        target.setStateKey("AVAILABLE");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);
        target.setAvatarUrl("http://example.com/product.png");
        target.setDescription("A product");
        target.setCreatedBy("system");

        Link link = createLink("LINK.OWNS", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(source);

        assertThat(json.has("targets")).isTrue();
        JsonNode linkNode = json.get("targets").get(0);
        assertThat(linkNode.get("id").asLong()).isEqualTo(50L);
        assertThat(linkNode.get("typeKey").asText()).isEqualTo("LINK.OWNS");
        assertThat(linkNode.get("name").asText()).isEqualTo("Test Link");
        assertThat(linkNode.get("description").asText()).isEqualTo("Link description");
        assertThat(linkNode.get("startDate").asText()).isEqualTo("2024-01-15T10:30:00Z");
        assertThat(linkNode.get("endDate").asText()).isEqualTo("2024-12-31T23:59:59Z");

        // target is a DTO object with same fields as SimpleLinkSerializer
        JsonNode targetNode = linkNode.get("target");
        assertThat(targetNode.isObject()).isTrue();
        assertThat(targetNode.get("id").asLong()).isEqualTo(200L);
        assertThat(targetNode.get("key").asText()).isEqualTo("product-1");
        assertThat(targetNode.get("typeKey").asText()).isEqualTo("PRODUCT");
        assertThat(targetNode.get("stateKey").asText()).isEqualTo("AVAILABLE");
        assertThat(targetNode.get("name").asText()).isEqualTo("Test Product");
        assertThat(targetNode.get("avatarUrl").asText()).isEqualTo("http://example.com/product.png");
        assertThat(targetNode.get("description").asText()).isEqualTo("A product");
        assertThat(targetNode.get("createdBy").asText()).isEqualTo("system");

        // source is just ID via @JsonIdentityReference on LinkDto.source
        assertThat(linkNode.get("source").isNumber()).isTrue();
        assertThat(linkNode.get("source").asLong()).isEqualTo(100L);
    }

    /**
     * Target entity's collections NOT in link target DTO.
     * LinkMapper.targetXmEntityToDto doesn't map collections.
     */
    @Test
    void serialize_targetEntityCollectionsNotInLink() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(200L);
        target.setTypeKey("PRODUCT");
        target.setKey("product-1");
        target.setName("Product with tags");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);

        Tag tag = createTag("TAG.FEATURED", "Featured", target);
        tag.setId(10L);
        target.setTags(new HashSet<>(Set.of(tag)));

        Location loc = new Location();
        loc.setId(11L);
        loc.setTypeKey("LOC.WAREHOUSE");
        loc.setName("Warehouse");
        loc.setXmEntity(target);
        target.setLocations(new HashSet<>(Set.of(loc)));

        Link link = createLink("LINK.REF", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(source);

        JsonNode targetNode = json.get("targets").get(0).get("target");
        assertThat(targetNode.has("tags")).isFalse();
        assertThat(targetNode.has("locations")).isFalse();
        assertThat(targetNode.has("attachments")).isFalse();
        assertThat(targetNode.has("comments")).isFalse();
        assertThat(targetNode.has("ratings")).isFalse();
        assertThat(targetNode.has("calendars")).isFalse();
        assertThat(targetNode.has("targets")).isFalse();
        assertThat(targetNode.has("sources")).isFalse();
        // version and updatedBy also NOT mapped by LinkMapper.targetXmEntityToDto
        assertThat(targetNode.has("version")).isFalse();
        assertThat(targetNode.has("updatedBy")).isFalse();
    }

    @Test
    void serialize_linkSourceAsIdNumber() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(300L);
        target.setTypeKey("TARGET.TYPE");
        target.setKey("t-1");
        target.setName("Target");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);

        Link link = createLink("LINK.TYPE", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(source);

        JsonNode linkNode = json.get("targets").get(0);
        assertThat(linkNode.get("source").isNumber()).isTrue();
        assertThat(linkNode.get("source").isObject()).isFalse();
        assertThat(linkNode.get("source").asLong()).isEqualTo(100L);
    }

    /**
     * In DTO: LinkDto has order field with Lombok getter -> order IS serialized.
     * (Unlike entity where SimpleLinkSerializer skips it)
     */
    @Test
    void serialize_linkOrderIncludedInDto() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(400L);
        target.setTypeKey("T.TYPE");
        target.setKey("t-2");
        target.setName("Target");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);

        Link link = createLink("LINK.ORDERED", source, target);
        // Note: Link entity doesn't have a getter for order, but the mapper transfers it to LinkDto which does
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(source);

        JsonNode linkNode = json.get("targets").get(0);
        // order field present in DTO (unlike SimpleLinkSerializer which skips it)
        // Value may be null if not set on the entity
        assertThat(linkNode.has("order")).isTrue();
    }

    @Test
    void serialize_linkTargetWithNullFieldsExcluded() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(500L);
        target.setTypeKey("MINIMAL.TARGET");
        target.setKey("mt-1");
        target.setName("Minimal Target");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);

        Link link = createLink("LINK.TYPE", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(source);

        JsonNode targetNode = json.get("targets").get(0).get("target");
        assertThat(targetNode.has("id")).isTrue();
        assertThat(targetNode.has("key")).isTrue();
        assertThat(targetNode.has("typeKey")).isTrue();
        assertThat(targetNode.has("name")).isTrue();
        assertThat(targetNode.has("startDate")).isTrue();
        assertThat(targetNode.has("updateDate")).isTrue();
        // Null fields excluded
        assertNullOrAbsent(targetNode, "stateKey");
        assertNullOrAbsent(targetNode, "endDate");
        assertNullOrAbsent(targetNode, "avatarUrl");
        assertNullOrAbsent(targetNode, "description");
        assertNullOrAbsent(targetNode, "createdBy");
        assertNullOrAbsent(targetNode, "removed");
    }

    // ==================== GROUP 4: Serialization - Data Map ====================

    @Test
    void serialize_dataWithNestedObjects() throws Exception {
        XmEntity entity = createMinimalEntity();
        Map<String, Object> data = new HashMap<>();
        data.put("address", Map.of("city", "Kyiv", "zip", "01001"));
        data.put("scores", List.of(95, 87, 100));
        data.put("active", true);
        data.put("count", 42);
        data.put("rate", 3.14);
        data.put("label", "premium");
        entity.setData(data);

        JsonNode json = entityToJson(entity);

        JsonNode dataNode = json.get("data");
        assertThat(dataNode).isNotNull();
        assertThat(dataNode.get("address").get("city").asText()).isEqualTo("Kyiv");
        assertThat(dataNode.get("scores").size()).isEqualTo(3);
        assertThat(dataNode.get("active").asBoolean()).isTrue();
        assertThat(dataNode.get("count").asInt()).isEqualTo(42);
        assertThat(dataNode.get("rate").asDouble()).isEqualTo(3.14);
        assertThat(dataNode.get("label").asText()).isEqualTo("premium");
    }

    @Test
    void serialize_emptyDataMapPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setData(new HashMap<>());

        JsonNode json = entityToJson(entity);

        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").isObject()).isTrue();
        assertThat(json.get("data").size()).isEqualTo(0);
    }

    @Test
    void serialize_dataWithDeeplyNestedStructure() throws Exception {
        XmEntity entity = createMinimalEntity();
        Map<String, Object> data = new HashMap<>();
        data.put("level1", Map.of("level2", Map.of("level3", Map.of("deepValue", "found"))));
        data.put("mixedList", List.of("string", 42, true, Map.of("nested", "inList")));
        data.put("nullableField", null);
        entity.setData(data);

        JsonNode json = entityToJson(entity);

        JsonNode dataNode = json.get("data");
        assertThat(dataNode.get("level1").get("level2").get("level3").get("deepValue").asText())
            .isEqualTo("found");
        assertThat(dataNode.get("mixedList").get(0).asText()).isEqualTo("string");
        assertThat(dataNode.get("mixedList").get(1).asInt()).isEqualTo(42);
        assertThat(dataNode.get("mixedList").get(2).asBoolean()).isTrue();
        assertThat(dataNode.get("mixedList").get(3).get("nested").asText()).isEqualTo("inList");
    }

    @Test
    void serialize_linkTargetIncludesDataField() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(600L);
        target.setTypeKey("TARGET.TYPE");
        target.setKey("t-data");
        target.setName("Target with data");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);
        Map<String, Object> targetData = new HashMap<>();
        targetData.put("color", "red");
        targetData.put("priority", 1);
        target.setData(targetData);

        Link link = createLink("LINK.TYPE", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = entityToJson(source);

        JsonNode targetNode = json.get("targets").get(0).get("target");
        assertThat(targetNode.has("data")).isTrue();
        assertThat(targetNode.get("data").get("color").asText()).isEqualTo("red");
        assertThat(targetNode.get("data").get("priority").asInt()).isEqualTo(1);
    }

    // ==================== GROUP 5: Deserialization (JSON -> DTO -> Entity) ====================

    /**
     * Deserialize source as number: JSON -> XmEntityDto (resolver finds entity) -> mapper -> XmEntity.
     * After mapper, source becomes shallow entity (ID only).
     */
    @Test
    void deserialize_linkSourceAsNumber() throws Exception {
        XmEntity savedSource = persistEntity("ACCOUNT", "src-1", "Source Entity");

        String json = String.format("""
            {
              "typeKey": "ACCOUNT",
              "key": "acc-deser-1",
              "name": "Deserialized Account",
              "targets": [
                {
                  "typeKey": "LINK.TYPE",
                  "target": {
                    "typeKey": "TARGET.TYPE",
                    "key": "tgt-1",
                    "name": "Target Entity"
                  },
                  "source": %d
                }
              ]
            }
            """, savedSource.getId());

        XmEntity result = jsonToEntity(json);

        assertThat(result.getTypeKey()).isEqualTo("ACCOUNT");
        assertThat(result.getTargets()).hasSize(1);
        Link link = result.getTargets().iterator().next();
        assertThat(link.getTypeKey()).isEqualTo("LINK.TYPE");
        // After mapper: source is shallow entity with only ID
        assertThat(link.getSource()).isNotNull();
        assertThat(link.getSource().getId()).isEqualTo(savedSource.getId());
        // target deserialized as full object through mapper
        assertThat(link.getTarget()).isNotNull();
        assertThat(link.getTarget().getTypeKey()).isEqualTo("TARGET.TYPE");
    }

    /**
     * Deserialize source as object: both forms resolve to shallow entity after mapper.
     */
    @Test
    void deserialize_linkSourceAsObject() throws Exception {
        XmEntity savedSource = persistEntity("ACCOUNT", "src-2", "Source Entity 2");

        String json = String.format("""
            {
              "typeKey": "ACCOUNT",
              "key": "acc-deser-2",
              "name": "Deserialized Account 2",
              "targets": [
                {
                  "typeKey": "LINK.TYPE",
                  "target": {
                    "typeKey": "TARGET.TYPE",
                    "key": "tgt-2",
                    "name": "Target Entity 2"
                  },
                  "source": {"id": %d}
                }
              ]
            }
            """, savedSource.getId());

        XmEntity result = jsonToEntity(json);

        assertThat(result.getTargets()).hasSize(1);
        Link link = result.getTargets().iterator().next();
        // After mapper: source is shallow entity with only ID
        assertThat(link.getSource()).isNotNull();
        assertThat(link.getSource().getId()).isEqualTo(savedSource.getId());
    }

    /**
     * Sources deserializable via @JsonProperty on setter in XmEntityDto.
     */
    @Test
    void deserialize_sourcesAllowedViaJsonPropertySetter() throws Exception {
        XmEntity savedSource = persistEntity("ACCOUNT", "src-3", "The Source");

        String json = String.format("""
            {
              "typeKey": "ACCOUNT",
              "key": "acc-deser-3",
              "name": "Entity with sources",
              "sources": [
                {
                  "typeKey": "LINK.BACK",
                  "startDate": "2024-01-15T10:30:00Z",
                  "source": {"id": %d}
                }
              ]
            }
            """, savedSource.getId());

        XmEntity result = jsonToEntity(json);

        assertThat(result.getSources()).isNotNull();
        assertThat(result.getSources()).hasSize(1);
        Link sourceLink = result.getSources().iterator().next();
        assertThat(sourceLink.getTypeKey()).isEqualTo("LINK.BACK");
    }

    @Test
    void deserialize_minimalJson() throws Exception {
        String json = """
            {
              "typeKey": "ACCOUNT",
              "key": "minimal-key",
              "name": "Minimal Entity"
            }
            """;

        XmEntity result = jsonToEntity(json);

        assertThat(result.getTypeKey()).isEqualTo("ACCOUNT");
        assertThat(result.getKey()).isEqualTo("minimal-key");
        assertThat(result.getName()).isEqualTo("Minimal Entity");
        assertThat(result.getId()).isNull();
        assertThat(result.getStateKey()).isNull();
        assertThat(result.getEndDate()).isNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.isRemoved()).isNull();
        assertThat(result.getCreatedBy()).isNull();
        assertThat(result.getTags()).isEmpty();
        assertThat(result.getLocations()).isEmpty();
        assertThat(result.getAttachments()).isEmpty();
    }

    @Test
    void deserialize_complexDataMap() throws Exception {
        String json = """
            {
              "typeKey": "ACCOUNT",
              "key": "data-key",
              "name": "Entity with data",
              "data": {
                "name": "John",
                "age": 30,
                "active": true,
                "score": 99.5,
                "address": { "city": "Kyiv", "zip": "01001" },
                "tags": ["vip", "premium"],
                "metadata": null
              }
            }
            """;

        XmEntity result = jsonToEntity(json);

        Map<String, Object> data = result.getData();
        assertThat(data).isNotNull();
        assertThat(data.get("name")).isEqualTo("John");
        assertThat(data.get("age")).isEqualTo(30);
        assertThat(data.get("active")).isEqualTo(true);
        assertThat(data.get("score")).isEqualTo(99.5);

        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) data.get("address");
        assertThat(address.get("city")).isEqualTo("Kyiv");

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) data.get("tags");
        assertThat(tags).containsExactly("vip", "premium");

        assertThat(data.containsKey("metadata")).isTrue();
        assertThat(data.get("metadata")).isNull();
    }

    @Test
    void deserialize_avatarUrlSetsCorrectFields() throws Exception {
        String json = """
            {
              "typeKey": "ACCOUNT",
              "key": "avatar-key",
              "name": "Entity with avatar",
              "avatarUrl": "http://cdn.example.com/img/avatar.png"
            }
            """;

        XmEntity result = jsonToEntity(json);

        assertThat(result.getAvatarUrl()).isEqualTo("http://cdn.example.com/img/avatar.png");
        assertThat(result.getAvatarUrlRelative()).isEqualTo("http://cdn.example.com/img/avatar.png");
    }

    @Test
    void deserialize_unknownFieldsIgnored() throws Exception {
        String json = """
            {
              "typeKey": "ACCOUNT",
              "key": "unknown-fields",
              "name": "Entity",
              "nonExistentField": "should be ignored",
              "anotherUnknown": 12345
            }
            """;

        XmEntity result = jsonToEntity(json);

        assertThat(result.getTypeKey()).isEqualTo("ACCOUNT");
        assertThat(result.getKey()).isEqualTo("unknown-fields");
        assertThat(result.getName()).isEqualTo("Entity");
    }

    /**
     * Tags with xmEntity as number: after mapper, xmEntity becomes shallow entity.
     */
    @Test
    void deserialize_tagsWithXmEntityAsNumber() throws Exception {
        XmEntity savedOwner = persistEntity("ACCOUNT", "owner-1", "Owner");

        String json = String.format("""
            {
              "typeKey": "ACCOUNT",
              "key": "tags-deser",
              "name": "Entity with tags",
              "tags": [
                {
                  "typeKey": "TAG.VIP",
                  "name": "VIP",
                  "startDate": "2024-01-15T10:30:00Z",
                  "xmEntity": %d
                }
              ]
            }
            """, savedOwner.getId());

        XmEntity result = jsonToEntity(json);

        assertThat(result.getTags()).hasSize(1);
        Tag tag = result.getTags().iterator().next();
        assertThat(tag.getTypeKey()).isEqualTo("TAG.VIP");
        assertThat(tag.getName()).isEqualTo("VIP");
        // After mapper: xmEntity is shallow entity with ID only
        assertThat(tag.getXmEntity()).isNotNull();
        assertThat(tag.getXmEntity().getId()).isEqualTo(savedOwner.getId());
    }

    /**
     * Roundtrip: XmEntity -> DTO -> JSON -> DTO -> XmEntity.
     */
    @Test
    void roundtrip_entityWithData() throws Exception {
        XmEntity original = createMinimalEntity();
        Map<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", 100);
        data.put("nested", Map.of("a", "b"));
        original.setData(data);
        original.setStateKey("ACTIVE");
        original.setDescription("roundtrip test");

        // Entity -> DTO -> JSON
        XmEntityDto dto = xmEntityMapper.toDto(original);
        String json = objectMapper.writeValueAsString(dto);
        // JSON -> DTO -> Entity
        XmEntityDto restoredDto = objectMapper.readValue(json, XmEntityDto.class);
        XmEntity restored = xmEntityMapper.toEntity(restoredDto);

        assertThat(restored.getId()).isEqualTo(original.getId());
        assertThat(restored.getTypeKey()).isEqualTo(original.getTypeKey());
        assertThat(restored.getKey()).isEqualTo(original.getKey());
        assertThat(restored.getName()).isEqualTo(original.getName());
        assertThat(restored.getStateKey()).isEqualTo(original.getStateKey());
        assertThat(restored.getDescription()).isEqualTo(original.getDescription());
        assertThat(restored.getStartDate()).isEqualTo(original.getStartDate());
        assertThat(restored.getUpdateDate()).isEqualTo(original.getUpdateDate());
        assertThat(restored.getData()).isEqualTo(original.getData());
    }

    /**
     * AvatarUrl roundtrip through DTO layer:
     * - Inbound: JSON "avatarUrl" -> DTO -> Entity (sets both avatarUrlRelative and avatarUrlFull)
     * - AvatarUrlListener.prePersist strips full URL to relative filename
     * - AvatarUrlListener.postLoad reconstructs full URL from relative
     * - Outbound: Entity -> DTO -> JSON "avatarUrl" (full URL from getAvatarUrl())
     *
     * This test verifies the DTO mapping preserves the avatar URL contract
     * without relying on the DB persistence (AvatarUrlListener is tested separately).
     */
    @Test
    void roundtrip_avatarUrl() throws Exception {
        String fullUrl = "http://example.com/avatar.png";
        XmEntity original = createMinimalEntity();
        original.setAvatarUrl(fullUrl);

        // Verify: Entity -> DTO -> JSON produces "avatarUrl" with full URL
        XmEntityDto dto = xmEntityMapper.toDto(original);
        assertThat(dto.getAvatarUrl()).isEqualTo(fullUrl);

        String json = objectMapper.writeValueAsString(dto);
        JsonNode jsonNode = objectMapper.readTree(json);
        assertThat(jsonNode.get("avatarUrl").asText()).isEqualTo(fullUrl);
        assertThat(jsonNode.has("avatarUrlRelative")).isFalse();
        assertThat(jsonNode.has("avatarUrlFull")).isFalse();

        // Verify: JSON -> DTO -> Entity sets both internal entity fields
        // (so AvatarUrlListener.prePersist can strip the URL correctly)
        XmEntityDto restoredDto = objectMapper.readValue(json, XmEntityDto.class);
        assertThat(restoredDto.getAvatarUrl()).isEqualTo(fullUrl);

        XmEntity restored = xmEntityMapper.toEntity(restoredDto);
        // Entity.setAvatarUrl() sets both avatarUrlRelative and avatarUrlFull
        assertThat(restored.getAvatarUrlRelative()).isEqualTo(fullUrl);
        assertThat(restored.getAvatarUrl()).isEqualTo(fullUrl);

        // Simulate what AvatarUrlListener.prePersist does: strip to relative
        // (the listener runs on JPA lifecycle, not tested here — tested in AvatarUrlListenerIntTest)
        // After prePersist: avatarUrlRelative = "avatar.png" (filename only if URL matched pattern)
        // After postLoad: avatarUrlFull = prefix + "avatar.png" (reconstructed)
    }

    @Test
    void serialize_functionContextIgnoredTransientFields() throws Exception {
        XmEntity entity = createMinimalEntity();

        FunctionContext fc = new FunctionContext();
        fc.setId(10L);
        fc.setKey("fc-ignored");
        fc.setTypeKey("FC.TYPE");
        fc.setStartDate(START);
        fc.setBinaryDataField("someField");
        fc.setOnlyData(true);
        fc.setBinaryDataType("application/octet-stream");
        Map<String, Object> fcData = new HashMap<>();
        fcData.put("output", "result");
        fc.setData(fcData);
        fc.setXmEntity(entity);
        entity.setFunctionContexts(new HashSet<>(Set.of(fc)));

        JsonNode json = entityToJson(entity);

        JsonNode fcNode = json.get("functionContexts").get(0);
        assertThat(fcNode.get("key").asText()).isEqualTo("fc-ignored");
        assertThat(fcNode.get("data").get("output").asText()).isEqualTo("result");
        assertThat(fcNode.has("binaryDataField")).isFalse();
        assertThat(fcNode.has("onlyData")).isFalse();
    }

    @Test
    void serialize_ratingVotesIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();

        Rating rating = new Rating();
        rating.setId(1L);
        rating.setTypeKey("RATING.STARS");
        rating.setValue(4.5);
        rating.setStartDate(START);
        rating.setXmEntity(entity);

        Vote vote = new Vote();
        vote.setId(1L);
        vote.setUserKey("user1");
        vote.setValue(5.0);
        vote.setEntryDate(START);
        rating.addVotes(vote);

        entity.setRatings(new HashSet<>(Set.of(rating)));

        JsonNode json = entityToJson(entity);

        JsonNode ratingNode = json.get("ratings").get(0);
        assertThat(ratingNode.get("value").asDouble()).isEqualTo(4.5);
        assertThat(ratingNode.has("votes")).isFalse();
    }

    @Test
    void serialize_commentRepliesIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();

        Comment parent = createComment("Parent comment", entity);
        parent.setId(1L);

        Comment reply = createComment("Reply", entity);
        reply.setId(2L);
        reply.setComment(parent);
        parent.addReplies(reply);

        entity.setComments(new HashSet<>(Set.of(parent)));

        JsonNode json = entityToJson(entity);

        JsonNode commentNode = json.get("comments").get(0);
        assertThat(commentNode.get("message").asText()).isEqualTo("Parent comment");
        assertThat(commentNode.has("replies")).isFalse();
    }

    @Test
    void serialize_versionZeroIsPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setVersion(0);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("version")).isTrue();
        assertThat(json.get("version").asInt()).isEqualTo(0);
    }

    @Test
    void serialize_versionPositiveIncluded() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setVersion(5);

        JsonNode json = entityToJson(entity);

        assertThat(json.has("version")).isTrue();
        assertThat(json.get("version").asInt()).isEqualTo(5);
    }

    @Test
    void serialize_calendarWithEvents() throws Exception {
        XmEntity entity = createMinimalEntity();

        Calendar cal = new Calendar();
        cal.setId(1L);
        cal.setTypeKey("CAL.WORK");
        cal.setName("Work Calendar");
        cal.setStartDate(START);
        cal.setXmEntity(entity);

        Event event = new Event();
        event.setId(1L);
        event.setTypeKey("EVENT.MEETING");
        event.setDescription("Daily standup");
        event.setStartDate(START);
        event.setEndDate(END);
        event.setCalendar(cal);
        event.setAssigned(entity);
        cal.setEvents(new HashSet<>(Set.of(event)));

        entity.setCalendars(new HashSet<>(Set.of(cal)));

        JsonNode json = entityToJson(entity);

        JsonNode calNode = json.get("calendars").get(0);
        assertThat(calNode.get("name").asText()).isEqualTo("Work Calendar");
        assertThat(calNode.has("events")).isTrue();

        JsonNode eventNode = calNode.get("events").get(0);
        assertThat(eventNode.get("typeKey").asText()).isEqualTo("EVENT.MEETING");
        assertThat(eventNode.get("description").asText()).isEqualTo("Daily standup");
        // assigned (XmEntity) serialized as ID via @JsonIdentityReference on EventDto
        assertThat(eventNode.get("assigned").isNumber()).isTrue();
        assertThat(eventNode.get("assigned").asLong()).isEqualTo(100L);
    }
}
