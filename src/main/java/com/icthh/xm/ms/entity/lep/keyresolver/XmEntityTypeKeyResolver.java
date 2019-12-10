package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.stereotype.Component;

@Component
public class XmEntityTypeKeyResolver extends AppendLepKeyResolver {
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        XmEntity xmEntity = getRequiredParam(method, "xmEntity", XmEntity.class);
        String translatedXmEntityTypeKey = translateToLepConvention(xmEntity.getTypeKey());
        return new String[] {
            translatedXmEntityTypeKey
        };
    }
}
