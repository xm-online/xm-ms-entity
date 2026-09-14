package com.icthh.xm.ms.entity.service.search.db.dialect;

import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B_TEXT;
import static com.icthh.xm.ms.entity.config.jsonb.PostgresJsonFunctionContributor.JSON_QUERY;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Postgres: {@code xm_json_query(data, '$.path')} renders {@code jsonb_path_query_first(data, '$.path'::jsonpath)}
 * (see {@link com.icthh.xm.ms.entity.config.jsonb.PostgresJsonFunctionContributor}) and returns jsonb, so
 * comparisons are jsonb-to-jsonb and numbers compare numerically without any type hint. Literals are wrapped
 * with {@code to_jsonb}, which is why {@code operand} is not needed here. The expression is the one the tenant
 * db patches build btree indexes on, so filters and sorts by a data path can use those indexes.
 */
@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:postgresql:')")
public class PostgresJsonValueStrategy implements JsonValueStrategy {

    @Override
    public Expression<?> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath, Object operand) {
        return cb.function(JSON_QUERY, String.class, dataColumn, cb.literal(jsonPath));
    }

    @Override
    public Expression<?> literal(CriteriaBuilder cb, Object value) {
        // to_jsonb(?::text) for strings, to_jsonb(?) for numbers and booleans (JDBC binds them typed)
        String function = value instanceof String ? TO_JSON_B_TEXT : TO_JSON_B;
        return cb.function(function, String.class, cb.literal(value));
    }

    @Override
    public List<Expression<?>> orderExpressions(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        // jsonb ordering already compares numbers as numbers
        return List.of(jsonValue(cb, dataColumn, jsonPath, null));
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        // Hibernate's built-in json_value renders jsonb_path_query_first(...) #>> '{}' on Postgres: text without quotes
        return cb.function("json_value", String.class, dataColumn, cb.literal(jsonPath));
    }

    /**
     * {@code jsonb_path_exists(data, '$.path[*]?(@ == $v)', jsonb_build_object('v', ?))}. The path is an inlined
     * literal (an untyped literal resolves to jsonpath, a bound varchar would not); the value is a bind parameter
     * whose JDBC type decides the JSON type it is compared as.
     */
    @Override
    public Predicate arrayHas(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath, Object value) {
        Expression<String> vars = cb.function("jsonb_build_object", String.class,
            cb.literal("v"), ((HibernateCriteriaBuilder) cb).value(value));
        return cb.isTrue(cb.function("jsonb_path_exists", Boolean.class,
            dataColumn, cb.literal(jsonPath + "[*]?(@ == $v)"), vars));
    }
}
