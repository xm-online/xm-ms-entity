package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplate;
import java.util.List;
import org.springframework.stereotype.Component;

/** LEP key from the {@code template} parameter's key. */
@Component
public class JpqlTemplateKeyResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("template", JpqlTemplate.class).getKey());
    }
}
