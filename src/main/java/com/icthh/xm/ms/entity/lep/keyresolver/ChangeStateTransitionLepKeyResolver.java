package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

/**
 * The {@link ChangeStateTransitionLepKeyResolver} class.
 */
@Component
public class ChangeStateTransitionLepKeyResolver extends AppendLepKeyResolver {

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
        String translatedPrevStateKey = translateToLepConvention(getRequiredStrParam(method, "prevStateKey"));
        String translatedNextStateKey = translateToLepConvention(getRequiredStrParam(method, "nextStateKey"));

        return new String[] {
            translatedXmEntityTypeKey,
            translatedPrevStateKey,
            translatedNextStateKey
        };
    }

}
