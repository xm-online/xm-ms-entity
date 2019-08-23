package com.icthh.xm.ms.entity.service.impl;

import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UiActionSpec;
import com.icthh.xm.ms.entity.service.ISpecVisitor;
import org.apache.poi.util.StringUtil;

import java.util.Set;

public class SpecVisitorServiceImpl implements ISpecVisitor/*<IUiEvaluatedSpec>*/ {

    public Set<UiActionSpec> visit(TypeSpec spec) {
        //Make calculation here from perspective Role + Implementation
        if (StringUtil.endsWithIgnoreCase(spec.getKey(), "0")) {
            return Sets.newHashSet(UiActionSpec.readOnly());
        } else {
            return Sets.newHashSet(UiActionSpec.all());
        }
    }

    public Set<UiActionSpec> visit(TagSpec spec) {
        if (StringUtil.endsWithIgnoreCase(spec.getKey(), "0")) {
            return Sets.newHashSet(UiActionSpec.readOnly());
        } else {
            return Sets.newHashSet(UiActionSpec.all());
        }
    }

}
