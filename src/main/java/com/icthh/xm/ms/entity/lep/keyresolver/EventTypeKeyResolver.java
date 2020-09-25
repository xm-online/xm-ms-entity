package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.Event;
import org.springframework.stereotype.Component;

@Component
public class EventTypeKeyResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        Event event = getRequiredParam(method, "event", Event.class);
        String translatedEventTypeKey = translateToLepConvention(event.getTypeKey());
        return new String[] {
            translatedEventTypeKey
        };
    }
}
