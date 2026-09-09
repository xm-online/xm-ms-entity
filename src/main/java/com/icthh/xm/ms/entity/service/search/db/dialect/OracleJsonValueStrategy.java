package com.icthh.xm.ms.entity.service.search.db.dialect;

import static com.icthh.xm.commons.migration.db.jsonb.CustomDialect.JSON_QUERY;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Oracle: {@code json_query(data, '$.path')} renders {@code json_value(...)} which returns text.
 * Limitation: numeric range operators compare textually.
 */
@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:oracle:')")
public class OracleJsonValueStrategy implements JsonValueStrategy {

    @Override
    public Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return cb.function(JSON_QUERY, String.class, dataColumn, cb.literal(jsonPath));
    }

    @Override
    public Expression<?> literal(CriteriaBuilder cb, Object value) {
        return cb.literal(String.valueOf(value));
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return jsonValue(cb, dataColumn, jsonPath);
    }
}
