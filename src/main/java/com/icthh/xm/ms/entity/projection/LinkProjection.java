package com.icthh.xm.ms.entity.projection;

public interface LinkProjection {

    Long getId();
    String getTypeKey();
    XmEntityId getTarget();
    XmEntityId getSource();

}
