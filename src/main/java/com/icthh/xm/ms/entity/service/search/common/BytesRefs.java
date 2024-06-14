package com.icthh.xm.ms.entity.service.search.common;

import org.apache.lucene.util.BytesRef;

public class BytesRefs {

    public static BytesRef toBytesRef(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BytesRef) {
            return (BytesRef) value;
        }
        return new BytesRef(value.toString());
    }
}
