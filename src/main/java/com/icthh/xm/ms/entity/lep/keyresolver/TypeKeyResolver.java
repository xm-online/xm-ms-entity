package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import java.util.Collections;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return Collections.singletonList(method.getParameter("typeKey", String.class));
    }
}
