package com.icthh.xm.ms.entity.domain.serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleLinkSerializerUnitTest extends AbstractJupiterUnitTest {

    private static final Instant START = Instant.parse("2024-01-15T10:30:00Z");
    private static final Instant END = Instant.parse("2024-12-31T23:59:59Z");

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeXmEntityTargetsWithLegacyJacksonMapper() throws Exception {
        XmEntity source = sourceEntity();
        Link link = link(source, targetEntity());
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(source));

        JsonNode linkNode = json.get("targets").get(0);
        assertThat(linkNode.get("id").asLong()).isEqualTo(50L);
        assertThat(linkNode.get("typeKey").asText()).isEqualTo("LINK.OWNS");
        assertThat(linkNode.get("source").asLong()).isEqualTo(100L);

        JsonNode targetNode = linkNode.get("target");
        assertThat(targetNode.get("id").asLong()).isEqualTo(200L);
        assertThat(targetNode.get("key").asText()).isEqualTo("product-1");
        assertThat(targetNode.get("typeKey").asText()).isEqualTo("PRODUCT");
        assertThat(targetNode.get("stateKey").asText()).isEqualTo("AVAILABLE");
        assertThat(targetNode.get("name").asText()).isEqualTo("Test Product");
    }

    @Test
    void shouldSerializeXmEntityTargetsWhenLinkSourceIsNull() throws Exception {
        XmEntity source = sourceEntity();
        Link link = link(null, targetEntity());
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(source));

        JsonNode linkNode = json.get("targets").get(0);
        assertThat(linkNode.has("source")).isFalse();
        assertThat(linkNode.get("target").get("id").asLong()).isEqualTo(200L);
    }

    @Test
    void shouldSerializeXmEntityTargetsWhenLinkTargetIsNull() throws Exception {
        XmEntity source = sourceEntity();
        Link link = link(source, null);
        source.setTargets(new HashSet<>(Set.of(link)));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(source));

        JsonNode linkNode = json.get("targets").get(0);
        assertThat(linkNode.get("source").asLong()).isEqualTo(100L);
        assertThat(linkNode.get("target").isObject()).isTrue();
        assertThat(linkNode.get("target").isEmpty()).isTrue();
    }

    private static XmEntity sourceEntity() {
        XmEntity source = new XmEntity();
        source.setId(100L);
        source.setTypeKey("ACCOUNT");
        source.setKey("account-1");
        source.setName("Test Account");
        source.setStartDate(START);
        return source;
    }

    private static XmEntity targetEntity() {
        XmEntity target = new XmEntity();
        target.setId(200L);
        target.setTypeKey("PRODUCT");
        target.setKey("product-1");
        target.setName("Test Product");
        target.setStateKey("AVAILABLE");
        target.setStartDate(START);
        return target;
    }

    private static Link link(XmEntity source, XmEntity target) {
        Link link = new Link();
        link.setId(50L);
        link.setTypeKey("LINK.OWNS");
        link.setName("Test Link");
        link.setDescription("Link description");
        link.setStartDate(START);
        link.setEndDate(END);
        link.setSource(source);
        link.setTarget(target);
        return link;
    }
}
