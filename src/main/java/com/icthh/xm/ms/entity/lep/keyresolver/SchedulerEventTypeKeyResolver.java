package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.stereotype.Component;

@Component
public class SchedulerEventTypeKeyResolver extends AppendLepKeyResolver {
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        ScheduledEvent scheduledEvent = getRequiredParam(method, "scheduledEvent", ScheduledEvent.class);
        String translatedXmEntityTypeKey = translateToLepConvention(scheduledEvent.getTypeKey());
        return new String[] {
            translatedXmEntityTypeKey
        };
    }
}
