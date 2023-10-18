package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.ms.entity.domain.Event;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventTypeKeyResolver implements LepKeyResolver {
    @Override
    public List<String> segments(LepMethod method) {
        return List.of(
            method.getParameter("event", Event.class).getTypeKey()
        );
    }
}
