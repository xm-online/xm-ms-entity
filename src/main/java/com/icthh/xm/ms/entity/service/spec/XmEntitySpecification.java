package com.icthh.xm.ms.entity.service.spec;

import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import java.util.LinkedHashMap;

public record XmEntitySpecification(
    String filePath,
    XmEntitySpec spec,
    LinkedHashMap<String, TypeSpec> types
){}
