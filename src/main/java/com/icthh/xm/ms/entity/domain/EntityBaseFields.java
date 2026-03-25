package com.icthh.xm.ms.entity.domain;

import java.util.Map;

public interface EntityBaseFields extends WithTypeKey {
    Long getId();
    String getKey();
    String getName();
    String getStateKey();
    Map<String, Object> getData();
}
