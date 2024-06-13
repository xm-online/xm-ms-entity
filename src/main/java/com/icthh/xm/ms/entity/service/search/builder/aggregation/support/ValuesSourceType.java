package com.icthh.xm.ms.entity.service.search.builder.aggregation.support;

import java.util.Locale;

public enum ValuesSourceType {
    ANY,
    NUMERIC;


    private ValuesSourceType() { }

    public String value() {
        return this.name().toLowerCase(Locale.ROOT);
    }

}
