package com.icthh.xm.ms.entity.service.search.db.filter;

import java.util.List;

/**
 * One parsed filter entry: {@code <field>.<op>} with its value(s).
 * {@code field} is either a whitelisted XmEntity column or a {@code data.<path>} json path.
 */
public record FilterCondition(String field, FilterOperator operator, List<Object> values) {

    public boolean isDataField() {
        return DataPath.isDataPath(field);
    }

    /** {@code data.a.b} → {@code $.a.b} (SQL/JSON path used by {@code json_query}). */
    public String dataJsonPath() {
        return DataPath.toJsonPath(field);
    }

    public Object singleValue() {
        return values.isEmpty() ? null : values.get(0);
    }
}
