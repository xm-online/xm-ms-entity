package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.config.SqlCaptureStatementInspector;
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
        e1 = repository.save(newEntity(TYPE, "Alpha order", Map.of("orderNo", 1, "sub", Map.of("position", 5), "city", "Kyiv",
            "categories", List.of("tech", "ai", "claude"), "codes", List.of(7, 42))));
        e2 = repository.save(newEntity(TYPE, "Beta order", Map.of("orderNo", 2, "sub", Map.of("position", 7), "city", "Lviv",
            "categories", List.of("claude-code", "ai"), "codes", List.of("7"))));
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

    private boolean includesRemoved(Map<String, Object> body) {
        return XmEntityFilterSpecificationBuilder.includesRemoved(parser.parseBody(body));
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
    public void dataArrayHasElement() {
        assertThat(ids(filter(Map.of("data.categories.has", "claude")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.categories.has", "ai")))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        // exact element, not a substring: "claude" does not match "claude-code"
        assertThat(ids(filter(Map.of("data.categories.has", "clau")))).isEmpty();
        // contains on the same path is the substring match over the array text
        assertThat(ids(filter(Map.of("data.categories.contains", "claude")))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        // compared with the JSON type of the value: number 7 is not the string "7"
        assertThat(ids(filter(Map.of("data.codes.has", 7)))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.codes.has", "7")))).containsExactly(e2.getId());
        // a missing path is no match; a scalar at the path is a one-element array (lax json path mode)
        assertThat(ids(filter(Map.of("data.missing.has", "x")))).isEmpty();
        assertThat(ids(filter(Map.of("data.city.has", "Kyiv")))).containsExactly(e1.getId());
        // GET form: string values keep their inferred type
        assertThat(ids(builder.build(parser.parseQueryParams(Map.of("data.codes.has", List.of("42"))))))
            .containsExactly(e1.getId());
    }

    @Test
    public void hasIsRejectedForColumnFields() {
        assertThatThrownBy(() -> ids(filter(Map.of("name.has", "x"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("has");
    }

    /**
     * ee-entity's CREATE_JSONPATH_INDEX patch builds btree indexes on
     * {@code jsonb_path_query_first(data, '$.path'::jsonpath)}. Postgres uses an expression index only when the
     * query contains that exact expression, so the filters must not fall back to Hibernate's built-in json_query
     * (a scalar subquery over jsonb_path_query, which no index can serve).
     */
    @Test
    public void dataFiltersRenderTheIndexedJsonbExpression() {
        SqlCaptureStatementInspector.clear();
        ids(filter(Map.of("data.sub.position.eq", 7, "data.orderNo.gt", 1, "data.city.specified", true)));

        String sql = SqlCaptureStatementInspector.lastSelect();
        assertThat(sql)
            .containsPattern("jsonb_path_query_first\\(\\w+\\.data, '\\$\\.sub\\.position'::jsonpath\\)=to_jsonb\\(")
            .containsPattern("jsonb_path_query_first\\(\\w+\\.data, '\\$\\.orderNo'::jsonpath\\)>to_jsonb\\(")
            .containsPattern("jsonb_path_query_first\\(\\w+\\.data, '\\$\\.city'::jsonpath\\) is not null")
            .doesNotContain("jsonb_path_query(");
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
    public void startsWithAndEndsWithOnColumnsAndData() {
        assertThat(ids(filter(Map.of("name.startsWith", "alpha")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("name.endsWith", "ORDER")))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        assertThat(ids(filter(Map.of("name.startsWith", "lpha")))).isEmpty();
        assertThat(ids(filter(Map.of("data.city.startsWith", "ky")))).containsExactly(e1.getId());
        assertThat(ids(filter(Map.of("data.city.endsWith", "VIV")))).containsExactly(e2.getId());
        assertThat(ids(filter(Map.of("data.city.startsWith", "yiv")))).isEmpty();
        // the pattern is escaped, so wildcards typed by the client are literal
        assertThat(ids(filter(Map.of("name.startsWith", "%")))).isEmpty();
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

    /** Subtypes come from the spec, so a prefix with no declared type matches nothing instead of scanning by like. */
    @Test
    public void typeKeyWithoutDeclaredTypesMatchesNothing() {
        repository.save(newEntity("ORDER", "Parent", Map.of()));
        assertThat(repository.findAll(builder.<XmEntity>typeKey("NO_SUCH_TYPE", true, root -> root))).isEmpty();
    }

    @Test
    public void notRemovedExcludesSoftDeleted() {
        assertThat(ids(builder.notRemoved(root -> root))).containsExactlyInAnyOrder(e1.getId(), e2.getId());
        // only removed.eq=true opts into soft-deleted rows
        assertThat(includesRemoved(Map.of("removed.eq", true))).isTrue();
        assertThat(includesRemoved(Map.of("removed.eq", false))).isFalse();
        assertThat(includesRemoved(Map.of("removed.notEq", false))).isFalse();
        assertThat(includesRemoved(Map.of("removed.specified", true))).isFalse();
        assertThat(includesRemoved(Map.of("name.eq", "x"))).isFalse();
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
        // "invalid" must not silently become false and match non-removed rows
        assertThatThrownBy(() -> ids(filter(Map.of("removed.eq", "invalid"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("removed");
        assertThatThrownBy(() -> ids(filter(Map.of("data..orderNo.eq", 1))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("data.");
        // "data" itself is not a column: it is addressed through data.<path>
        assertThatThrownBy(() -> ids(filter(Map.of("data.eq", 1))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("data");
    }
}
