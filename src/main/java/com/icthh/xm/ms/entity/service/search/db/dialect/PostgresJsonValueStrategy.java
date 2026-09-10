package com.icthh.xm.ms.entity.service.search.db.dialect;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B;
import static com.icthh.xm.commons.migration.db.jsonb.CustomPostgreSQLDialect.TO_JSON_B_TEXT;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Postgres: {@code json_query(data, '$.path')} renders {@code jsonb_path_query_first(...)} and returns jsonb, so
 * comparisons are jsonb-to-jsonb and numbers compare numerically without any type hint. Literals are wrapped
 * with {@code to_jsonb}, which is why {@code operand} is not needed here.
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
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        // Hibernate's built-in json_value renders jsonb_path_query_first(...) #>> '{}' on Postgres: text without quotes
        return cb.function("json_value", String.class, dataColumn, cb.literal(jsonPath));
    }
}
