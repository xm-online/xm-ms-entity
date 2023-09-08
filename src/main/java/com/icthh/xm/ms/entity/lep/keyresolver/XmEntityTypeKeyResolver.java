package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class XmEntityTypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("xmEntity", XmEntity.class).getTypeKey());
    }
}
