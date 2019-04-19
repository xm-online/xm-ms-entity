package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TypeKeyResolver extends AppendLepKeyResolver {

    private final TypeKeyWithExtends typeKeyWithExtends;

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String typeKey = getRequiredParam(method, "typeKey", String.class);
        return typeKeyWithExtends.resolveWithTypeKeyInheritance(baseKey, typeKey,
                                                                tk -> new String[] {
                                                                    translateToLepConvention(tk)
                                                                });
    }
}
