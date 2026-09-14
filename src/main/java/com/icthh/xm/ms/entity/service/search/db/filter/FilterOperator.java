package com.icthh.xm.ms.entity.service.search.db.filter;

import java.util.Arrays;
import java.util.Optional;

public enum FilterOperator {
    EQ("eq"), NOT_EQ("notEq"), IN("in"), NOT_IN("notIn"), CONTAINS("contains"),
    SPECIFIED("specified"), GT("gt"), GTE("gte"), LT("lt"), LTE("lte"),
    /** JSON array at the data path has an element equal to the value; data fields only. */
    HAS("has");

    private final String suffix;

    FilterOperator(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean isMultiValue() {
        return this == IN || this == NOT_IN;
    }

    public static Optional<FilterOperator> bySuffix(String suffix) {
        return Arrays.stream(values()).filter(op -> op.suffix.equals(suffix)).findFirst();
    }
}
