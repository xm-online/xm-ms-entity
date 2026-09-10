package com.icthh.xm.ms.entity.config;

import com.icthh.xm.ms.entity.service.search.db.dialect.JsonValueStrategy;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Test-only stand-in for the H2 profile: lets the DB search bean graph start, but every jsonb operation
 * fails fast. jsonb filtering and sorting are covered by the Postgres (Testcontainers) tests.
 */
@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:h2:')")
public class H2JsonValueStrategy implements JsonValueStrategy {

    private static final String MESSAGE = "jsonb filtering is not implemented for H2; use the pg-test profile";

    @Override
    public Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        throw new NotImplementedException(MESSAGE);
    }

    @Override
    public Expression<?> literal(CriteriaBuilder cb, Object value) {
        throw new NotImplementedException(MESSAGE);
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        throw new NotImplementedException(MESSAGE);
    }
}
