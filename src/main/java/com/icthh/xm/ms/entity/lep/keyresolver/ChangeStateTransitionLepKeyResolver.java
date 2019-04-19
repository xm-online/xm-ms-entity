package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The {@link ChangeStateTransitionLepKeyResolver} class.
 */
@Component
@RequiredArgsConstructor
public class ChangeStateTransitionLepKeyResolver extends AppendLepKeyResolver {

    private final TypeKeyWithExtends typeKeyWithExtends;

    /**
     * LEP extension key specification:<br>
     * {@code ChangeState$$<xm-entity-type-key>$$<next-state-key>}
     * <p>
     * Add to method name {@code <xm-entity-type-key} and {@code <state-key>} to the end.
     *
     * @param baseKey        base LEP key (prefix), can be {@code null}
     * @param method         method data on what LEP call occurs
     * @param managerService LEP manager service
     * @return dynamic value of LEP base key
     */
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        String translatedXmEntityTypeKey = translateToLepConvention(getRequiredStrParam(method, "xmEntityTypeKey"));
        String translatedNextStateKey = translateToLepConvention(getRequiredStrParam(method, "nextStateKey"));
        String translatedPrevStateKey = translateToLepConvention(getRequiredStrParam(method, "prevStateKey"));
        return typeKeyWithExtends.resolveWithTypeKeyInheritance(baseKey, translatedXmEntityTypeKey,
                                                                tk -> new String[] {
                                                                    translateToLepConvention(tk),
                                                                    translatedPrevStateKey,
                                                                    translatedNextStateKey
                                                                });
    }

}
