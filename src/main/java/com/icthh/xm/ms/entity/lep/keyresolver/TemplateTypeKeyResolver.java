package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.stereotype.Component;

@Component
public class TemplateTypeKeyResolver extends AppendLepKeyResolver {
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String template = getRequiredParam(method, "template", String.class);
        String translatedTemplate = translateToLepConvention(template);
        return new String[] {
            translatedTemplate
        };
    }
}
