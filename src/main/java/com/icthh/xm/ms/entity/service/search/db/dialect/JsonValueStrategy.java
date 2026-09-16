package com.icthh.xm.ms.entity.service.search.db.dialect;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.List;

/** DB-specific pieces of jsonb filtering. One bean is active, selected by the datasource URL. */
public interface JsonValueStrategy {

    /**
     * Expression of the JSON value at {@code jsonPath} ({@code $.a.b}), comparable with {@link #literal}.
     *
     * @param operand the value this expression is compared with, or {@code null} when there is none
     *                (a null check, or an order by). Dialects that extract JSON as text use it to pick the
     *                SQL type, so numbers compare numerically rather than lexically.
     */
    Expression<?> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath, Object operand);

    /** Literal in the same representation as {@link #jsonValue} so {@code =}, {@code in}, {@code >} work. */
    Expression<?> literal(CriteriaBuilder cb, Object value);

    /**
     * The JSON array at {@code jsonPath} has an element equal to {@code value}, compared with the JSON type of
     * the value (string to string, number to number). Backs the {@code has} operator; the value is bound.
     * Lax JSON path mode: a scalar at the path counts as a one-element array, a missing path never matches.
     * A dialect that cannot render it throws a validation {@code BusinessException}.
     */
    Predicate arrayHas(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath, Object value);

    /** Text form of the JSON value for case-insensitive {@code contains}. */
    Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);

    /**
     * Expressions to order a {@code data.<path>} by. Ordering carries no compared value, so a dialect that
     * extracts JSON as text returns several keys: a typed one first, then a text fallback, otherwise numbers
     * would be ordered lexically and 10 would come before 2.
     */
    List<Expression<?>> orderExpressions(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);
}
