package com.icthh.xm.ms.entity.service.search.db.dialect;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;

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

    /** Text form of the JSON value for case-insensitive {@code contains}. */
    Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);
}
