package com.icthh.xm.ms.entity.util;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.service.XmEntityServiceImplUnitTest;

import java.util.function.Consumer;

public class EntityUtils {

    public static Long TEST_ID = 0L;
    public static String TEST_TYPE_KEY = XmEntityServiceImplUnitTest.TEST_TYPE_KEY;
    public static String TEST_KEY = "0-TEST";

    public static Consumer<XmEntity> defaultEntity() {
        return e -> {
            e.setId(TEST_ID);
            e.setTypeKey(TEST_TYPE_KEY);
            e.setKey(TEST_KEY);
        };
    }

    public static XmEntity newEntity() {
        return newEntity(defaultEntity());
    }

    public static XmEntity newEntity(Consumer<XmEntity> entity) {
        XmEntity e = new XmEntity();
        entity.accept(e);
        return e;
    }

    public static XmEntityIdKeyTypeKey projectionFromEntity(XmEntity e) {
        return new XmEntityIdKeyTypeKey() {
            @Override
            public Long getId() {
                return e.getId();
            }

            @Override
            public String getKey() {
                return e.getKey();
            }

            @Override
            public String getTypeKey() {
                return e.getTypeKey();
            }
        };
    }

}
