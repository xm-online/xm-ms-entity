package com.icthh.xm.ms.entity.service.search.builder;

import org.elasticsearch.common.Strings;

public class CommonTermsQueryBuilder {

    private final String fieldName;

    private final Object text;

    public CommonTermsQueryBuilder(String fieldName, Object text) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("field name is null or empty");
        }
        if (text == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        this.fieldName = fieldName;
        this.text = text;
    }
}
