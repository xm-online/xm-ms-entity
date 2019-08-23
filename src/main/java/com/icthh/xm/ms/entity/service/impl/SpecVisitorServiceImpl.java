package com.icthh.xm.ms.entity.service.impl;

import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.domain.spec.IUiEvaluatedSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UiActionSpec;
import com.icthh.xm.ms.entity.service.ISpecVisitor;
import org.apache.poi.util.StringUtil;

import java.util.Set;

public class SpecVisitorServiceImpl implements ISpecVisitor/*<IUiEvaluatedSpec>*/ {

    private Set<UiActionSpec> megaStrategy(IUiEvaluatedSpec spec) {

        return StringUtil.endsWithIgnoreCase(spec.getKey(), "0") ?
            Sets.newHashSet(UiActionSpec.readOnly()) :
            Sets.newHashSet(UiActionSpec.crud());

    }

    public Set<UiActionSpec> visit(TypeSpec spec) {
        //Make calculation here from perspective Role + Implementation
        return megaStrategy(spec);
    }

    public Set<UiActionSpec> visit(TagSpec spec) {
        //Make calculation here from perspective Role + Implementation
        return megaStrategy(spec);
    }

}
