package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.spec.IUiEvaluatedSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UiActionSpec;

import java.util.Set;

public interface ISpecVisitor {

    //Set<UiActionSpec> visit(IUiEvaluatedSpec spec);
    Set<UiActionSpec> visit(TypeSpec spec);
    Set<UiActionSpec> visit(TagSpec spec);

}
