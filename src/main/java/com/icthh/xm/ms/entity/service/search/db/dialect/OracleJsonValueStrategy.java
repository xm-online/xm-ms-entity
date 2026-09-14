package com.icthh.xm.ms.entity.service.search.db.dialect;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Oracle extracts JSON values as scalars, so the SQL type has to be chosen per comparison: Hibernate's typed
 * {@code jsonValue} renders {@code JSON_VALUE(data, '$.path' RETURNING <type>)}. Without the type Oracle would
 * compare numbers as text and rank {@code 10} before {@code 2}.
 *
 * <p>The type comes from the value being compared with. An expression without an operand (a null check) falls
 * back to text; ordering has no operand either and is handled by {@link #orderExpressions}.
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

    /**
     * Numeric key first, text key second: {@code JSON_VALUE(... RETURNING NUMBER)} yields null for values
     * that are not numbers, so numeric fields are ordered numerically and text fields fall back to the text key.
     */
    @Override
    public List<Expression<?>> orderExpressions(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        HibernateCriteriaBuilder builder = (HibernateCriteriaBuilder) cb;
        return List.of(builder.jsonValue(dataColumn, jsonPath, BigDecimal.class),
            builder.jsonValue(dataColumn, jsonPath, String.class));
    }

    @Override
    public Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath) {
        return ((HibernateCriteriaBuilder) cb).jsonValue(dataColumn, jsonPath, String.class);
    }

    /**
     * Not supported on Oracle: Hibernate cannot bind the compared value in the {@code json_exists} passing clause
     * (7.3), and an inlined value would force a hard parse per value. Use Postgres for array filters.
     */
    @Override
    public Predicate arrayHas(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath, Object value) {
        throw new BusinessException(ERR_VALIDATION, "Filter operator has is not supported on Oracle");
    }

    /** SQL type of the RETURNING clause, derived from the value the json value is compared with. */
    static Class<?> returningType(Object operand) {
        return switch (operand) {
            case Long ignored -> Long.class;
            case Integer ignored -> Long.class;
            case BigInteger ignored -> Long.class;
            case Double ignored -> BigDecimal.class;
            case Float ignored -> BigDecimal.class;
            case BigDecimal ignored -> BigDecimal.class;
            case Boolean ignored -> Boolean.class;
            case null, default -> String.class;
        };
    }
}
