package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TemplateTypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("template", String.class));
    }
}
