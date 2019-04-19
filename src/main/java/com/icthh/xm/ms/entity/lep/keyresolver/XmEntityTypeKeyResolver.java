package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.LepResource;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class XmEntityTypeKeyResolver extends AppendLepKeyResolver {

    private final TypeKeyWithExtends typeKeyWithExtends;

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        XmEntity xmEntity = getRequiredParam(method, "xmEntity", XmEntity.class);
        return typeKeyWithExtends.resolveWithTypeKeyInheritance(baseKey, xmEntity.getTypeKey(),
                                                                typeKey -> new String[] {
                                                                    translateToLepConvention(typeKey)
                                                                });
    }
}
