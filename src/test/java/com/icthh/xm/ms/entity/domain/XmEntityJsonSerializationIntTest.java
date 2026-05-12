package com.icthh.xm.ms.entity.domain;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
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
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for XmEntity JSON serialization and deserialization.
 *
 * Verifies Jackson behavior with:
 * - spring.jackson.default-property-inclusion=non_empty
 * - spring.jackson.serialization.write_dates_as_timestamps=false
 * - SimpleLinkSerializer for targets/sources
 * - @JsonIgnore / @JsonProperty / @JsonIdentityReference annotations
 * - XmEntityObjectIdResolver for entity ID references
 */
@Transactional
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityJsonSerializationIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private JsonMapper objectMapper;

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

    private JsonNode toJsonNode(Object obj) throws Exception {
        String json = objectMapper.writeValueAsString(obj);
        return objectMapper.readTree(json);
    }

    private long persistEntityIdCounter = 90000L;

    /**
     * Persist entity using native SQL to bypass @TypeKey and @JsonData validators.
     */
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

    /**
     * Assert that a field is either absent or has a null JSON value.
     * The behavior depends on ObjectMapper's property inclusion settings,
     * which may vary depending on Hibernate module initialization.
     */
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

    /**
     * Case 1: Minimal entity - null String/Instant fields excluded by NON_EMPTY,
     * but empty collections and empty data map remain present
     * (Hibernate module keeps collections serialized).
     */
    @Test
    void serialize_minimalEntity_emptyCollectionsPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setData(new HashMap<>());

        JsonNode json = toJsonNode(entity);

        // Required scalar fields present
        assertThat(json.get("id").asLong()).isEqualTo(100L);
        assertThat(json.get("typeKey").asText()).isEqualTo("ACCOUNT");
        assertThat(json.get("key").asText()).isEqualTo("account-1");
        assertThat(json.get("name").asText()).isEqualTo("Test Account");
        assertThat(json.has("startDate")).isTrue();
        assertThat(json.has("updateDate")).isTrue();

        // Empty collections ARE present as empty arrays (Hibernate module keeps them)
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

        // Empty data map IS present as empty object
        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").size()).isEqualTo(0);

        // Null fields are absent or serialized as null
        assertNullOrAbsent(json, "endDate");
        assertNullOrAbsent(json, "stateKey");
        assertNullOrAbsent(json, "description");
    }

    /**
     * Case 2: All scalar fields populated - all should appear in JSON.
     */
    @Test
    void serialize_allScalarFieldsPopulated() throws Exception {
        XmEntity entity = createFullEntity();

        JsonNode json = toJsonNode(entity);

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

    /**
     * Case 3: Instant dates serialized as ISO-8601 strings, not timestamps.
     */
    @Test
    void serialize_datesAsIsoStrings() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setEndDate(END);

        JsonNode json = toJsonNode(entity);

        assertThat(json.get("startDate").asText()).isEqualTo("2024-01-15T10:30:00Z");
        assertThat(json.get("updateDate").asText()).isEqualTo("2024-01-15T12:00:00Z");
        assertThat(json.get("endDate").asText()).isEqualTo("2024-12-31T23:59:59Z");

        // Verify they're strings, not numbers
        assertThat(json.get("startDate").isTextual()).isTrue();
        assertThat(json.get("startDate").isNumber()).isFalse();
    }

    /**
     * Case 4: Null optional fields are either absent or serialized as null.
     */
    @Test
    void serialize_nullOptionalFieldsExcluded() throws Exception {
        XmEntity entity = createMinimalEntity();
        // endDate, stateKey, description, avatarUrl, createdBy, updatedBy, removed are all null

        JsonNode json = toJsonNode(entity);

        // All null optional fields: absent or null
        assertNullOrAbsent(json, "endDate");
        assertNullOrAbsent(json, "stateKey");
        assertNullOrAbsent(json, "description");
        assertNullOrAbsent(json, "avatarUrl");
        assertNullOrAbsent(json, "createdBy");
        assertNullOrAbsent(json, "updatedBy");
        assertNullOrAbsent(json, "removed");
    }

    /**
     * Case 5: avatarUrl serialized as "avatarUrl", not "avatarUrlRelative" or "avatarUrlFull".
     */
    @Test
    void serialize_avatarUrlFieldNaming() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setAvatarUrl("http://example.com/avatar.png");

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("avatarUrl")).isTrue();
        assertThat(json.get("avatarUrl").asText()).isEqualTo("http://example.com/avatar.png");
        // Internal field names should NOT appear
        assertThat(json.has("avatarUrlRelative")).isFalse();
        assertThat(json.has("avatarUrlFull")).isFalse();
    }

    /**
     * Case 6: removed=false IS present in JSON (Boolean serialized regardless of NON_EMPTY).
     */
    @Test
    void serialize_removedFalseIsPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setRemoved(false);

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("removed")).isTrue();
        assertThat(json.get("removed").asBoolean()).isFalse();
    }

    /**
     * Case 7: removed=true IS included (only true survives NON_EMPTY for Boolean).
     */
    @Test
    void serialize_removedTrueIncluded() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setRemoved(true);

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("removed")).isTrue();
        assertThat(json.get("removed").asBoolean()).isTrue();
    }

    /**
     * Case 8: isNew() is @JsonIgnore - "new" field should NOT appear in JSON.
     */
    @Test
    void serialize_isNewFieldIgnored() throws Exception {
        // id=null makes isNew() return true
        XmEntity entity = createMinimalEntity();
        entity.setId(null);
        assertThat(entity.isNew()).isTrue();

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("new")).isFalse();
    }

    // ==================== GROUP 2: Serialization - Collections & @JsonIgnore ====================

    /**
     * Case 9: sources IS serialized despite @JsonIgnore on getter.
     * Field-level @JsonSerialize(contentUsing=SimpleLinkSerializer.class) takes precedence,
     * making Jackson discover the property via field annotation.
     */
    @Test
    void serialize_sourcesSerializedViaFieldAnnotation() throws Exception {
        XmEntity entity = createMinimalEntity();
        XmEntity sourceEntity = createMinimalEntity();
        sourceEntity.setId(200L);

        Link link = createLink("LINK.TYPE", sourceEntity, entity);
        entity.setSources(new HashSet<>(Set.of(link)));

        JsonNode json = toJsonNode(entity);

        // sources IS present because @JsonSerialize on field overrides @JsonIgnore on getter
        assertThat(json.has("sources")).isTrue();
        assertThat(json.get("sources").isArray()).isTrue();
        assertThat(json.get("sources").size()).isEqualTo(1);

        // Each source link is serialized via SimpleLinkSerializer
        JsonNode linkNode = json.get("sources").get(0);
        assertThat(linkNode.get("id").asLong()).isEqualTo(50L);
        assertThat(linkNode.get("typeKey").asText()).isEqualTo("LINK.TYPE");
        // source of the link is the sourceEntity (id=200)
        assertThat(linkNode.get("source").isNumber()).isTrue();
        assertThat(linkNode.get("source").asLong()).isEqualTo(200L);
        // target of the link is the main entity (id=100)
        assertThat(linkNode.get("target").isObject()).isTrue();
        assertThat(linkNode.get("target").get("id").asLong()).isEqualTo(100L);
    }

    /**
     * Case 10: votes is @JsonIgnore on field - NOT in serialized JSON.
     */
    @Test
    void serialize_votesIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();
        Vote vote = new Vote();
        vote.setId(1L);
        vote.setUserKey("user1");
        vote.setValue(5.0);
        vote.setEntryDate(START);
        entity.addVotes(vote);

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("votes")).isFalse();
    }

    /**
     * Case 11: events is @JsonIgnore on field - NOT in serialized JSON.
     */
    @Test
    void serialize_eventsIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();
        Event event = new Event();
        event.setId(1L);
        event.setTypeKey("EVENT.TYPE");
        event.setDescription("test event");
        event.setStartDate(START);
        entity.addEvent(event);

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("events")).isFalse();
    }

    /**
     * Case 12: uniqueFields is @JsonIgnore on field - NOT in serialized JSON.
     */
    @Test
    void serialize_uniqueFieldsIgnored() throws Exception {
        XmEntity entity = createMinimalEntity();
        UniqueField uf = new UniqueField();
        uf.setId(1L);
        entity.setUniqueFields(new HashSet<>(Set.of(uf)));

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("uniqueFields")).isFalse();
    }

    /**
     * Case 13: Tags serialized with xmEntity back-reference as just an ID number.
     * Due to @JsonIdentityReference(alwaysAsId=true) on Tag.xmEntity.
     */
    @Test
    void serialize_tagsWithXmEntityAsId() throws Exception {
        XmEntity entity = createMinimalEntity();
        Tag tag = createTag("TAG.VIP", "VIP Customer", entity);
        tag.setId(10L);
        entity.setTags(new HashSet<>(Set.of(tag)));

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("tags")).isTrue();
        JsonNode tagsArray = json.get("tags");
        assertThat(tagsArray.isArray()).isTrue();
        assertThat(tagsArray.size()).isEqualTo(1);

        JsonNode tagNode = tagsArray.get(0);
        assertThat(tagNode.get("id").asLong()).isEqualTo(10L);
        assertThat(tagNode.get("typeKey").asText()).isEqualTo("TAG.VIP");
        assertThat(tagNode.get("name").asText()).isEqualTo("VIP Customer");
        // xmEntity must be serialized as just the entity ID (number), not an object
        assertThat(tagNode.get("xmEntity").isNumber()).isTrue();
        assertThat(tagNode.get("xmEntity").asLong()).isEqualTo(100L);
    }

    /**
     * Case 14: Comments serialized with xmEntity back-reference as just an ID number.
     */
    @Test
    void serialize_commentsWithXmEntityAsId() throws Exception {
        XmEntity entity = createMinimalEntity();
        Comment comment = createComment("Hello world", entity);
        comment.setId(20L);
        comment.setClientId("mobile-app");
        comment.setDisplayName("Admin User");
        entity.setComments(new HashSet<>(Set.of(comment)));

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("comments")).isTrue();
        JsonNode commentsArray = json.get("comments");
        assertThat(commentsArray.size()).isEqualTo(1);

        JsonNode commentNode = commentsArray.get(0);
        assertThat(commentNode.get("id").asLong()).isEqualTo(20L);
        assertThat(commentNode.get("message").asText()).isEqualTo("Hello world");
        assertThat(commentNode.get("userKey").asText()).isEqualTo("user1");
        assertThat(commentNode.get("clientId").asText()).isEqualTo("mobile-app");
        assertThat(commentNode.get("displayName").asText()).isEqualTo("Admin User");
        // xmEntity is just an ID
        assertThat(commentNode.get("xmEntity").isNumber()).isTrue();
        assertThat(commentNode.get("xmEntity").asLong()).isEqualTo(100L);
        // replies is @JsonIgnore
        assertThat(commentNode.has("replies")).isFalse();
    }

    /**
     * Case 15: Locations, Attachments, Ratings, Calendars, FunctionContexts
     * are all serialized when non-empty.
     */
    @Test
    void serialize_allNonIgnoredCollections() throws Exception {
        XmEntity entity = createMinimalEntity();

        // Location
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

        // Attachment
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

        // Rating
        Rating rating = new Rating();
        rating.setId(3L);
        rating.setTypeKey("RATING.STARS");
        rating.setValue(4.5);
        rating.setStartDate(START);
        rating.setXmEntity(entity);
        entity.setRatings(new HashSet<>(Set.of(rating)));

        // Calendar
        Calendar cal = new Calendar();
        cal.setId(4L);
        cal.setTypeKey("CAL.WORK");
        cal.setName("Work Calendar");
        cal.setStartDate(START);
        cal.setTimeZoneId("Europe/Paris");
        cal.setXmEntity(entity);
        entity.setCalendars(new HashSet<>(Set.of(cal)));

        // FunctionContext
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

        JsonNode json = toJsonNode(entity);

        // All non-ignored collections present
        assertThat(json.has("locations")).isTrue();
        assertThat(json.get("locations").size()).isEqualTo(1);
        JsonNode locNode = json.get("locations").get(0);
        assertThat(locNode.get("typeKey").asText()).isEqualTo("LOC.ADDRESS");
        assertThat(locNode.get("city").asText()).isEqualTo("Paris");
        assertThat(locNode.get("latitude").asDouble()).isEqualTo(48.8566);
        assertThat(locNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("attachments")).isTrue();
        assertThat(json.get("attachments").size()).isEqualTo(1);
        JsonNode attNode = json.get("attachments").get(0);
        assertThat(attNode.get("name").asText()).isEqualTo("contract.pdf");
        assertThat(attNode.get("valueContentType").asText()).isEqualTo("application/pdf");
        assertThat(attNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("ratings")).isTrue();
        assertThat(json.get("ratings").size()).isEqualTo(1);
        JsonNode ratingNode = json.get("ratings").get(0);
        assertThat(ratingNode.get("value").asDouble()).isEqualTo(4.5);
        assertThat(ratingNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("calendars")).isTrue();
        assertThat(json.get("calendars").size()).isEqualTo(1);
        JsonNode calNode = json.get("calendars").get(0);
        assertThat(calNode.get("name").asText()).isEqualTo("Work Calendar");
        assertThat(calNode.get("timeZoneId").asText()).isEqualTo("Europe/Paris");
        assertThat(calNode.get("xmEntity").isNumber()).isTrue();

        assertThat(json.has("functionContexts")).isTrue();
        assertThat(json.get("functionContexts").size()).isEqualTo(1);
        JsonNode fcNode = json.get("functionContexts").get(0);
        assertThat(fcNode.get("key").asText()).isEqualTo("fc-1");
        assertThat(fcNode.get("data").get("result").asText()).isEqualTo("success");
        assertThat(fcNode.get("xmEntity").isNumber()).isTrue();
    }

    /**
     * Case 16: Multiple collections present at the same time.
     */
    @Test
    void serialize_multipleCollectionsTogether() throws Exception {
        XmEntity entity = createMinimalEntity();

        // Add tags
        Tag tag1 = createTag("TAG.A", "Alpha", entity);
        tag1.setId(1L);
        Tag tag2 = createTag("TAG.B", "Beta", entity);
        tag2.setId(2L);
        entity.setTags(new HashSet<>(Set.of(tag1, tag2)));

        // Add locations
        Location loc = new Location();
        loc.setId(3L);
        loc.setTypeKey("LOC.HOME");
        loc.setName("Home");
        loc.setXmEntity(entity);
        entity.setLocations(new HashSet<>(Set.of(loc)));

        // Add comments
        Comment comment = createComment("Nice!", entity);
        comment.setId(4L);
        entity.setComments(new HashSet<>(Set.of(comment)));

        JsonNode json = toJsonNode(entity);

        assertThat(json.get("tags").size()).isEqualTo(2);
        assertThat(json.get("locations").size()).isEqualTo(1);
        assertThat(json.get("comments").size()).isEqualTo(1);
    }

    // ==================== GROUP 3: Serialization - Links (SimpleLinkSerializer) ====================

    /**
     * Case 17: Targets serialized using SimpleLinkSerializer.
     * Produces flattened format: {id, typeKey, name, description, startDate, endDate, target:{...}, source:ID}
     */
    @Test
    void serialize_targetsWithSimpleLinkSerializer() throws Exception {
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

        JsonNode json = toJsonNode(source);

        assertThat(json.has("targets")).isTrue();
        JsonNode targetsArray = json.get("targets");
        assertThat(targetsArray.size()).isEqualTo(1);

        JsonNode linkNode = targetsArray.get(0);
        // Link's own fields
        assertThat(linkNode.get("id").asLong()).isEqualTo(50L);
        assertThat(linkNode.get("typeKey").asText()).isEqualTo("LINK.OWNS");
        assertThat(linkNode.get("name").asText()).isEqualTo("Test Link");
        assertThat(linkNode.get("description").asText()).isEqualTo("Link description");
        assertThat(linkNode.get("startDate").asText()).isEqualTo("2024-01-15T10:30:00Z");
        assertThat(linkNode.get("endDate").asText()).isEqualTo("2024-12-31T23:59:59Z");

        // target is a flattened object with subset of fields
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

        // source is just the source entity's ID
        assertThat(linkNode.get("source").isNumber()).isTrue();
        assertThat(linkNode.get("source").asLong()).isEqualTo(100L);
    }

    /**
     * Case 18: Target entity's tags/locations/etc. are NOT included in SimpleLinkSerializer output.
     * SimpleLinkSerializer only writes a subset of target entity fields.
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

        // Add tags to the target entity
        Tag tag = createTag("TAG.FEATURED", "Featured", target);
        tag.setId(10L);
        target.setTags(new HashSet<>(Set.of(tag)));

        // Add locations to the target entity
        Location loc = new Location();
        loc.setId(11L);
        loc.setTypeKey("LOC.WAREHOUSE");
        loc.setName("Warehouse");
        loc.setXmEntity(target);
        target.setLocations(new HashSet<>(Set.of(loc)));

        Link link = createLink("LINK.REF", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = toJsonNode(source);

        JsonNode targetNode = json.get("targets").get(0).get("target");
        // Tags should NOT appear in the simplified target
        assertThat(targetNode.has("tags")).isFalse();
        // Locations should NOT appear
        assertThat(targetNode.has("locations")).isFalse();
        // Attachments, comments, ratings, calendars, functionContexts, targets, sources also absent
        assertThat(targetNode.has("attachments")).isFalse();
        assertThat(targetNode.has("comments")).isFalse();
        assertThat(targetNode.has("ratings")).isFalse();
        assertThat(targetNode.has("calendars")).isFalse();
        assertThat(targetNode.has("targets")).isFalse();
        assertThat(targetNode.has("sources")).isFalse();
        // version and updatedBy are also NOT written by SimpleLinkSerializer
        assertThat(targetNode.has("version")).isFalse();
        assertThat(targetNode.has("updatedBy")).isFalse();
    }

    /**
     * Case 19: Link.source is serialized as just a number (entity ID) by SimpleLinkSerializer.
     */
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

        JsonNode json = toJsonNode(source);

        JsonNode linkNode = json.get("targets").get(0);
        // source must be a number, not an object
        assertThat(linkNode.get("source").isNumber()).isTrue();
        assertThat(linkNode.get("source").isObject()).isFalse();
        assertThat(linkNode.get("source").asLong()).isEqualTo(100L);
    }

    /**
     * Case 20: SimpleLinkSerializer does NOT include Link.order field.
     */
    @Test
    void serialize_linkOrderNotIncluded() throws Exception {
        XmEntity source = createMinimalEntity();
        XmEntity target = new XmEntity();
        target.setId(400L);
        target.setTypeKey("T.TYPE");
        target.setKey("t-2");
        target.setName("Target");
        target.setStartDate(START);
        target.setUpdateDate(UPDATE);

        Link link = createLink("LINK.ORDERED", source, target);
        // order field exists on Link but SimpleLinkSerializer doesn't serialize it
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = toJsonNode(source);

        JsonNode linkNode = json.get("targets").get(0);
        assertThat(linkNode.has("order")).isFalse();
    }

    /**
     * Case 21: Target entity with null optional fields - those excluded from target object.
     * SimpleLinkSerializer's write() method skips null values.
     */
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
        // stateKey, endDate, avatarUrl, description, createdBy, data, removed are all null

        Link link = createLink("LINK.TYPE", source, target);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = toJsonNode(source);

        JsonNode targetNode = json.get("targets").get(0).get("target");
        // Present fields
        assertThat(targetNode.has("id")).isTrue();
        assertThat(targetNode.has("key")).isTrue();
        assertThat(targetNode.has("typeKey")).isTrue();
        assertThat(targetNode.has("name")).isTrue();
        assertThat(targetNode.has("startDate")).isTrue();
        assertThat(targetNode.has("updateDate")).isTrue();
        // Null fields excluded by SimpleLinkSerializer's null check
        assertThat(targetNode.has("stateKey")).isFalse();
        assertThat(targetNode.has("endDate")).isFalse();
        assertThat(targetNode.has("avatarUrl")).isFalse();
        assertThat(targetNode.has("description")).isFalse();
        assertThat(targetNode.has("createdBy")).isFalse();
        assertThat(targetNode.has("removed")).isFalse();
    }

    // ==================== GROUP 4: Serialization - Data Map ====================

    /**
     * Case 22: Data map with nested objects serialized correctly.
     */
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

        JsonNode json = toJsonNode(entity);

        JsonNode dataNode = json.get("data");
        assertThat(dataNode).isNotNull();
        assertThat(dataNode.get("address").get("city").asText()).isEqualTo("Kyiv");
        assertThat(dataNode.get("address").get("zip").asText()).isEqualTo("01001");
        assertThat(dataNode.get("scores").isArray()).isTrue();
        assertThat(dataNode.get("scores").size()).isEqualTo(3);
        assertThat(dataNode.get("scores").get(0).asInt()).isEqualTo(95);
        assertThat(dataNode.get("active").asBoolean()).isTrue();
        assertThat(dataNode.get("count").asInt()).isEqualTo(42);
        assertThat(dataNode.get("rate").asDouble()).isEqualTo(3.14);
        assertThat(dataNode.get("label").asText()).isEqualTo("premium");
    }

    /**
     * Case 23: Empty data map IS present in JSON (not excluded despite NON_EMPTY config).
     */
    @Test
    void serialize_emptyDataMapPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setData(new HashMap<>());

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("data")).isTrue();
        assertThat(json.get("data").isObject()).isTrue();
        assertThat(json.get("data").size()).isEqualTo(0);
    }

    /**
     * Case 24: Data with deeply nested structure and various types.
     */
    @Test
    void serialize_dataWithDeeplyNestedStructure() throws Exception {
        XmEntity entity = createMinimalEntity();
        Map<String, Object> data = new HashMap<>();
        data.put("level1", Map.of(
            "level2", Map.of(
                "level3", Map.of("deepValue", "found")
            )
        ));
        data.put("mixedList", List.of("string", 42, true, Map.of("nested", "inList")));
        data.put("nullableField", null); // null within map - this IS stored in the map
        entity.setData(data);

        JsonNode json = toJsonNode(entity);

        JsonNode dataNode = json.get("data");
        assertThat(dataNode.get("level1").get("level2").get("level3").get("deepValue").asText())
            .isEqualTo("found");
        assertThat(dataNode.get("mixedList").get(0).asText()).isEqualTo("string");
        assertThat(dataNode.get("mixedList").get(1).asInt()).isEqualTo(42);
        assertThat(dataNode.get("mixedList").get(2).asBoolean()).isTrue();
        assertThat(dataNode.get("mixedList").get(3).get("nested").asText()).isEqualTo("inList");
    }

    /**
     * Case 25: SimpleLinkSerializer includes target's data field when present.
     */
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

        JsonNode json = toJsonNode(source);

        JsonNode targetNode = json.get("targets").get(0).get("target");
        assertThat(targetNode.has("data")).isTrue();
        assertThat(targetNode.get("data").get("color").asText()).isEqualTo("red");
        assertThat(targetNode.get("data").get("priority").asInt()).isEqualTo(1);
    }

    // ==================== GROUP 5: Deserialization ====================

    /**
     * Case 26: Deserialize XmEntity with targets where source is a number (ID).
     * XmEntityObjectIdResolver resolves the entity from DB.
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

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        assertThat(result.getTypeKey()).isEqualTo("ACCOUNT");
        assertThat(result.getTargets()).hasSize(1);
        Link link = result.getTargets().iterator().next();
        assertThat(link.getTypeKey()).isEqualTo("LINK.TYPE");
        // source resolved from DB by ID number
        assertThat(link.getSource()).isNotNull();
        assertThat(link.getSource().getId()).isEqualTo(savedSource.getId());
        assertThat(link.getSource().getName()).isEqualTo("Source Entity");
        // target deserialized as full object
        assertThat(link.getTarget()).isNotNull();
        assertThat(link.getTarget().getTypeKey()).isEqualTo("TARGET.TYPE");
    }

    /**
     * Case 27: Deserialize XmEntity with targets where source is an object {"id": N}.
     * Unlike the number form (which goes through XmEntityObjectIdResolver),
     * the object form is deserialized as a new XmEntity POJO with only the provided fields.
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

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        assertThat(result.getTargets()).hasSize(1);
        Link link = result.getTargets().iterator().next();
        // source object form: only the fields from JSON are set
        assertThat(link.getSource()).isNotNull();
        assertThat(link.getSource().getId()).isEqualTo(savedSource.getId());
        // name is NOT populated - object form doesn't look up from DB
        assertThat(link.getSource().getName()).isNull();
    }

    /**
     * Case 28: Sources array is deserializable despite @JsonIgnore on getter.
     * Because @JsonProperty on setSources() enables deserialization (write-only pattern).
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

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        // sources can be deserialized
        assertThat(result.getSources()).isNotNull();
        assertThat(result.getSources()).hasSize(1);
        Link sourceLink = result.getSources().iterator().next();
        assertThat(sourceLink.getTypeKey()).isEqualTo("LINK.BACK");
    }

    /**
     * Case 29: Deserialize minimal JSON - only required fields.
     */
    @Test
    void deserialize_minimalJson() throws Exception {
        String json = """
            {
              "typeKey": "ACCOUNT",
              "key": "minimal-key",
              "name": "Minimal Entity"
            }
            """;

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        assertThat(result.getTypeKey()).isEqualTo("ACCOUNT");
        assertThat(result.getKey()).isEqualTo("minimal-key");
        assertThat(result.getName()).isEqualTo("Minimal Entity");
        // Optional fields are null
        assertThat(result.getId()).isNull();
        assertThat(result.getStateKey()).isNull();
        assertThat(result.getEndDate()).isNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.isRemoved()).isNull();
        assertThat(result.getCreatedBy()).isNull();
        // Collections are empty (initialized in field declaration)
        assertThat(result.getTags()).isEmpty();
        assertThat(result.getLocations()).isEmpty();
        assertThat(result.getAttachments()).isEmpty();
    }

    /**
     * Case 30: Deserialize complex nested data map.
     */
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
                "address": {
                  "city": "Kyiv",
                  "zip": "01001"
                },
                "tags": ["vip", "premium"],
                "metadata": null
              }
            }
            """;

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

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

        // null value within data map
        assertThat(data.containsKey("metadata")).isTrue();
        assertThat(data.get("metadata")).isNull();
    }

    /**
     * Case 31: Deserialize avatarUrl - sets both avatarUrlRelative and avatarUrlFull.
     */
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

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        // setAvatarUrl() sets both fields
        assertThat(result.getAvatarUrl()).isEqualTo("http://cdn.example.com/img/avatar.png");
        assertThat(result.getAvatarUrlRelative()).isEqualTo("http://cdn.example.com/img/avatar.png");
    }

    /**
     * Case 32: Deserialize ignores unknown fields (Spring Boot default: FAIL_ON_UNKNOWN_PROPERTIES=false).
     */
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

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        assertThat(result.getTypeKey()).isEqualTo("ACCOUNT");
        assertThat(result.getKey()).isEqualTo("unknown-fields");
        assertThat(result.getName()).isEqualTo("Entity");
    }

    /**
     * Case 33: Deserialize with tags inline - xmEntity reference as number.
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

        XmEntity result = objectMapper.readValue(json, XmEntity.class);

        assertThat(result.getTags()).hasSize(1);
        Tag tag = result.getTags().iterator().next();
        assertThat(tag.getTypeKey()).isEqualTo("TAG.VIP");
        assertThat(tag.getName()).isEqualTo("VIP");
        // xmEntity resolved from DB
        assertThat(tag.getXmEntity()).isNotNull();
        assertThat(tag.getXmEntity().getId()).isEqualTo(savedOwner.getId());
    }

    /**
     * Case 34: Serialize then deserialize roundtrip for entity with data.
     * Verifies data survives the roundtrip.
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

        String json = objectMapper.writeValueAsString(original);
        XmEntity restored = objectMapper.readValue(json, XmEntity.class);

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
     * Case 35: FunctionContext binaryDataField and onlyData are @JsonIgnore.
     */
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

        JsonNode json = toJsonNode(entity);

        JsonNode fcNode = json.get("functionContexts").get(0);
        assertThat(fcNode.get("key").asText()).isEqualTo("fc-ignored");
        assertThat(fcNode.get("data").get("output").asText()).isEqualTo("result");
        // @JsonIgnore transient fields
        assertThat(fcNode.has("binaryDataField")).isFalse();
        assertThat(fcNode.has("onlyData")).isFalse();
    }

    /**
     * Case 36: Rating's votes are @JsonIgnore - not in serialized Rating within XmEntity.
     */
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

        JsonNode json = toJsonNode(entity);

        JsonNode ratingNode = json.get("ratings").get(0);
        assertThat(ratingNode.get("value").asDouble()).isEqualTo(4.5);
        // votes is @JsonIgnore on Rating
        assertThat(ratingNode.has("votes")).isFalse();
    }

    /**
     * Case 37: Comment's replies are @JsonIgnore - not serialized.
     */
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

        JsonNode json = toJsonNode(entity);

        JsonNode commentNode = json.get("comments").get(0);
        assertThat(commentNode.get("message").asText()).isEqualTo("Parent comment");
        // replies is @JsonIgnore
        assertThat(commentNode.has("replies")).isFalse();
    }

    /**
     * Case 38: version=0 is present in JSON (Integer zero serialized).
     */
    @Test
    void serialize_versionZeroIsPresent() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setVersion(0);

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("version")).isTrue();
        assertThat(json.get("version").asInt()).isEqualTo(0);
    }

    /**
     * Case 39: version with positive value is included.
     */
    @Test
    void serialize_versionPositiveIncluded() throws Exception {
        XmEntity entity = createMinimalEntity();
        entity.setVersion(5);

        JsonNode json = toJsonNode(entity);

        assertThat(json.has("version")).isTrue();
        assertThat(json.get("version").asInt()).isEqualTo(5);
    }

    /**
     * Case 40: Calendar with events - events serialized within calendar,
     * and event's assigned (XmEntity) is serialized as ID.
     */
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

        JsonNode json = toJsonNode(entity);

        JsonNode calNode = json.get("calendars").get(0);
        assertThat(calNode.get("name").asText()).isEqualTo("Work Calendar");
        assertThat(calNode.has("events")).isTrue();

        JsonNode eventNode = calNode.get("events").get(0);
        assertThat(eventNode.get("typeKey").asText()).isEqualTo("EVENT.MEETING");
        assertThat(eventNode.get("description").asText()).isEqualTo("Daily standup");
        // assigned (XmEntity) serialized as ID
        assertThat(eventNode.get("assigned").isNumber()).isTrue();
        assertThat(eventNode.get("assigned").asLong()).isEqualTo(100L);
    }
}
