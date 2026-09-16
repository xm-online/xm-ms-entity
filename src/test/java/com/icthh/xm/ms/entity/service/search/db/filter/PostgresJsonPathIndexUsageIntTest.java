package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.config.SqlCaptureStatementInspector;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regression guard for the tenant db patches of xm-ms-ee-entity: {@code CREATE_JSONPATH_INDEX} builds a btree on
 * {@code jsonb_path_query_first(data, '$.path'::jsonpath)}, and PostgreSQL uses an expression index only for the
 * identical expression. The test creates such an index (and a trigram one for {@code contains}), runs the real
 * filters and checks the plan of the generated SQL. Everything is rolled back with the test transaction.
 */
@Transactional
public class PostgresJsonPathIndexUsageIntTest extends AbstractPostgresIntTest {

    private static final String TYPE = "SILENT";

    @Autowired private XmEntityFilterSpecificationBuilder builder;
    @Autowired private FilterParser parser;
    @Autowired private XmEntityRepository repository;
    @Autowired private EntityManager entityManager;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    public void seedAndIndex() {
        pushDbSearchSpec();
        for (int i = 0; i < 300; i++) {
            repository.save(newEntity(TYPE, "e" + i, Map.of("orderNo", i, "city", "city" + i % 20)));
        }
        entityManager.flush();
        jdbc = new JdbcTemplate(dataSource);
        // what JsonPathCreateIndexPatch generates, schema prefix aside
        jdbc.execute("CREATE INDEX idx_test_orderno ON xm_entity (jsonb_path_query_first(data, '$.orderNo'::jsonpath))");
        // a GeneralCreateIndexPatch for contains: pg_trgm lives in public, the tenant search_path does not include it
        jdbc.execute("CREATE INDEX idx_test_city_trgm ON xm_entity USING gin "
            + "((jsonb_path_query_first(data, '$.city'::jsonpath) #>> '{}') public.gin_trgm_ops)");
        jdbc.execute("ANALYZE xm_entity");
        // 300 rows would otherwise be cheaper to scan; the planner must still be able to choose the index
        jdbc.execute("SET LOCAL enable_seqscan = off");
    }

    /** Runs the filter, then explains the SQL Hibernate generated for it, bind markers replaced by the given values. */
    private String planOf(Map<String, Object> filter, String... bindValues) {
        SqlCaptureStatementInspector.clear();
        repository.findAll(Specification.where(builder.<XmEntity>typeKey(TYPE, false, root -> root))
            .and(builder.build(parser.parseBody(filter))));
        String sql = SqlCaptureStatementInspector.lastSelect();
        for (String value : bindValues) {
            sql = sql.replaceFirst("\\?", "'" + value + "'");
        }
        assertThat(sql).doesNotContain("?");
        return String.join("\n", jdbc.queryForList("EXPLAIN " + sql, String.class));
    }

    @Test
    public void equalityRangeAndInUseTheJsonPathIndex() {
        assertThat(planOf(Map.of("data.orderNo.eq", 7), TYPE)).contains("Index Scan using idx_test_orderno");
        assertThat(planOf(Map.of("data.orderNo.gt", 290), TYPE)).contains("Index Scan using idx_test_orderno");
        assertThat(planOf(Map.of("data.orderNo.in", List.of(1, 2)), TYPE)).contains("Index Scan using idx_test_orderno");
    }

    @Test
    public void containsUsesTheTrigramIndexOnTheSameExpression() {
        assertThat(planOf(Map.of("data.city.contains", "ty17"), TYPE, "%ty17%"))
            .contains("Bitmap Index Scan on idx_test_city_trgm");
    }
}
