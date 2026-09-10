package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class XmEntityFilterSpecificationBuilderIntTest extends AbstractPostgresIntTest {

    private static final String TYPE = "SILENT";

    @Autowired
    private XmEntityFilterSpecificationBuilder builder;
    @Autowired
    private FilterParser parser;
    @Autowired
    private XmEntityRepository repository;

    private XmEntity e1;
    private XmEntity e2;
    private XmEntity e3;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        e1 = repository.save(newEntity(TYPE, "Alpha order", Map.of("orderNo", 1, "sub", Map.of("position", 5), "city", "Kyiv")));
        e2 = repository.save(newEntity(TYPE, "Beta order", Map.of("orderNo", 2, "sub", Map.of("position", 7), "city", "Lviv")));
        e3 = repository.save(newEntity(TYPE, "Gamma", Map.of("orderNo", 10)));
        e3.setRemoved(true);
        e3.setStateKey("CLOSED");
        e3.setStartDate(Instant.parse("2020-01-01T00:00:00Z"));
        repository.save(e3);
    }

    private List<Long> ids(Specification<XmEntity> spec) {
        return repository.findAll(Specification.where(builder.<XmEntity>typeKey(TYPE, false, root -> root)).and(spec))
            .stream().map(XmEntity::getId).toList();
    }

    private Specification<XmEntity> filter(Map<String, Object> body) {
        return builder.build(parser.parseBody(body));
    }

    @Test
    public void dataNumericInAndEq() {
        assertThat(ids(filter(Map.of("data.orderNo.in", List.of(1, 2))))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(ids(filter(Map.of("data.sub.position.eq", 7)))).containsExactly(e2.getId());
        assertThat(ids(filter(Map.of("data.sub.position.notEq", 7)))).containsExactly(e1.getId());
    }

    @Test
    public void dataNumericRangeComparesAsNumbers() {
        // textual comparison would put "10" before "2"; numeric jsonb comparison must not
        assertThat(ids(filter(Map.of("data.orderNo.gt", 2)))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("data.orderNo.lte", 2)))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    public void dataStringEqAndContains() {
        assertThat(ids(filter(Map.of("data.city.eq", "Kyiv")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.city.contains", "YI")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.city.notIn", List.of("Kyiv"))))).containsExactly(e2.getId());
    }

    @Test
    public void dataSpecified() {
        assertThat(ids(filter(Map.of("data.sub.position.specified", false)))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("data.city.specified", true)))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    public void columnFieldsAreTyped() {
        assertThat(ids(filter(Map.of("name.contains", "order")))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(ids(filter(Map.of("stateKey.eq", "CLOSED")))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("startDate.lt", "2021-01-01T00:00:00Z")))).containsExactly(e3.getId());
        assertThat(ids(filter(Map.of("id.in", List.of(e1.getId().toString()))))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("removed.eq", true)))).containsExactly(e3.getId());
    }

    @Test
    public void typeKeyWithAndWithoutSubTypes() {
        XmEntity sub = repository.save(newEntity("ORDER.EXPRESS", "Sub", Map.of()));
        XmEntity parent = repository.save(newEntity("ORDER", "Parent", Map.of()));
        XmEntity lookalike = repository.save(newEntity("ORDERX", "Lookalike", Map.of()));

        List<Long> withSub = repository.findAll(builder.<XmEntity>typeKey("ORDER", true, root -> root)).stream().map(XmEntity::getId).toList();
        List<Long> exact = repository.findAll(builder.<XmEntity>typeKey("ORDER", false, root -> root)).stream().map(XmEntity::getId).toList();

        assertThat(withSub).contains(parent.getId(), sub.getId()).doesNotContain(lookalike.getId());
        assertThat(exact).contains(parent.getId()).doesNotContain(sub.getId(), lookalike.getId());
    }

    @Test
    public void notRemovedExcludesSoftDeleted() {
        assertThat(ids(builder.notRemoved(root -> root))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(XmEntityFilterSpecificationBuilder.hasRemovedCondition(parser.parseBody(Map.of("removed.eq", true)))).isTrue();
        assertThat(XmEntityFilterSpecificationBuilder.hasRemovedCondition(parser.parseBody(Map.of("name.eq", "x")))).isFalse();
    }

    @Test
    public void rejectsUnknownColumnAndBadValue() {
        assertThatThrownBy(() -> ids(filter(Map.of("nope.eq", "x"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("nope");
        // every persisted scalar column is filterable, e.g. version
        assertThat(ids(filter(Map.of("version.gte", 0)))).containsExactlyInAnyOrder(e1.getId(), e2.getId(), e3.getId());
        assertThatThrownBy(() -> ids(filter(Map.of("startDate.eq", "not-a-date"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("startDate");
        assertThatThrownBy(() -> ids(filter(Map.of("id.eq", "abc"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("id");
    }
}
