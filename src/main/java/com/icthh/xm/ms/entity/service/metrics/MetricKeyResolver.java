package com.icthh.xm.ms.entity.service.metrics;

import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.lep.keyresolver.AppendLepKeyResolver;
import org.springframework.stereotype.Component;

@Component
public class MetricKeyResolver extends AppendLepKeyResolver {
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String metricName = getRequiredParam(method, "metricName", String.class);
        String translatedMetricName = translateToLepConvention(metricName);
        return new String[] {
            translatedMetricName
        };
    }
}
