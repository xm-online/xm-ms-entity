package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import java.util.List;
import org.springframework.stereotype.Component;

/** LEP key from the {@code entityTypeKey} and {@code linkTypeKey} String parameters. */
@Component
public class EntityTypeKeyAndLinkTypeKeyResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("entityTypeKey", String.class), method.getParameter("linkTypeKey", String.class));
    }
}
