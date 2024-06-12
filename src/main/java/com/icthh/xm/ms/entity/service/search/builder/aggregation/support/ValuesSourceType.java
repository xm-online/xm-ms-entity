package com.icthh.xm.ms.entity.service.search.builder.aggregation.support;

import org.elasticsearch.common.ParseField;

import java.util.Locale;

public enum ValuesSourceType {
    ANY,
    NUMERIC,
    BYTES,
    GEOPOINT;

    public static final ParseField VALUE_SOURCE_TYPE = new ParseField("value_source_type");

    private ValuesSourceType() { }

    public static ValuesSourceType fromString(String name) {
        return valueOf(name.trim().toUpperCase(Locale.ROOT));
    }

    public String value() {
        return this.name().toLowerCase(Locale.ROOT);
    }

}
