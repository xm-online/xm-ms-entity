package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.stereotype.Component;

@Component
public class LocationTypeKeyResolver extends AppendLepKeyResolver {
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        Location location = getRequiredParam(method, "location", Location.class);
        String translatedLocationTypeKey = translateToLepConvention(location.getTypeKey());
        return new String[] {
            translatedLocationTypeKey
        };
    }
}
