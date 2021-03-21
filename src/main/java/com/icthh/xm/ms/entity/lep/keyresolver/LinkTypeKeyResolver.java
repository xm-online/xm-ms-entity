package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Link;
import org.springframework.stereotype.Component;

@Component
public class LinkTypeKeyResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        Link link = getRequiredParam(method, "link", Link.class);
        String translatedEventTypeKey = translateToLepConvention(link.getTypeKey());
        return new String[] {
            translatedEventTypeKey
        };
    }
}
