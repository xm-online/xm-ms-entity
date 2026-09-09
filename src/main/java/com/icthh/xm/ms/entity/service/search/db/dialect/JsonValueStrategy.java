package com.icthh.xm.ms.entity.service.search.db.dialect;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;

/** DB-specific pieces of jsonb filtering. One bean is active, selected by the datasource URL. */
public interface JsonValueStrategy {

    /** Expression of the JSON value at {@code jsonPath} ({@code $.a.b}), comparable with {@link #literal}. */
    Expression<String> jsonValue(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);

    /** Literal in the same representation as {@link #jsonValue} so {@code =}, {@code in}, {@code >} work. */
    Expression<?> literal(CriteriaBuilder cb, Object value);

    /** Text form of the JSON value for case-insensitive {@code contains}. */
    Expression<String> jsonText(CriteriaBuilder cb, Path<?> dataColumn, String jsonPath);
}
