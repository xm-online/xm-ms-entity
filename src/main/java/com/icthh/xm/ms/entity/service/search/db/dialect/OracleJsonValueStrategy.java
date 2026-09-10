package com.icthh.xm.ms.entity.service.search.db.dialect;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Oracle extracts JSON values as scalars, so the SQL type has to be chosen per comparison: Hibernate's typed
 * {@code jsonValue} renders {@code JSON_VALUE(data, '$.path' RETURNING <type>)}. Without the type Oracle would
 * compare numbers as text and rank {@code 10} before {@code 2}.
 *
 * <p>The type comes from the value being compared with. An expression without an operand (a null check, or an
 * {@code order by}) falls back to text, so ordering by a numeric json path is lexical on Oracle.
 */
@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:oracle:')")
public class OracleJsonValueStrategy implements JsonValueStrategy {

    @Override
    public Expression<?> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath, Object operand) {
        return ((HibernateCriteriaBuilder) cb).jsonValue(dataColumn, jsonPath, returningType(operand));
    }

    @Override
    public Expression<?> literal(CriteriaBuilder cb, Object value) {
        return cb.literal(value);
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return ((HibernateCriteriaBuilder) cb).jsonValue(dataColumn, jsonPath, String.class);
    }

    /** SQL type of the RETURNING clause, derived from the value the json value is compared with. */
    static Class<?> returningType(Object operand) {
        return switch (operand) {
            case Long ignored -> Long.class;
            case Integer ignored -> Long.class;
            case BigInteger ignored -> Long.class;
            case Double ignored -> Double.class;
            case Float ignored -> Double.class;
            case BigDecimal ignored -> BigDecimal.class;
            case Boolean ignored -> Boolean.class;
            case null, default -> String.class;
        };
    }
}
