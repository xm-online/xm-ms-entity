package com.icthh.xm.ms.entity.domain.spec;

import com.icthh.xm.ms.entity.service.ISpecVisitor;

import java.util.Set;

public interface IUiEvaluatedSpec extends ISpec{

    Set<UiActionSpec> getUiActionSpec();

    void setUiActionSpec(Set<UiActionSpec> actionsSpec);

    void accept(ISpecVisitor visitor);

}
