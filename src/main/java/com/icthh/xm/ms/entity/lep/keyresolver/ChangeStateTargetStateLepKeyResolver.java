package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The {@link ChangeStateTargetStateLepKeyResolver} class.
 */
@Component
@RequiredArgsConstructor
public class ChangeStateTargetStateLepKeyResolver extends AppendLepKeyResolver {

    /**
     * Method parameter name for {@code stateKey}.
     */
    private static final String PARAM_NEXT_STATE_KEY = "nextStateKey";

    /**
     * Method parameter name for {@code xmEntityTypeKey}.
     */
    private static final String PARAM_TYPE_KEY = "xmEntityTypeKey";

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
        String translatedXmEntityTypeKey = translateToLepConvention(getRequiredStrParam(method, PARAM_TYPE_KEY));
        String translatedNextStateKey = translateToLepConvention(getRequiredStrParam(method, PARAM_NEXT_STATE_KEY));

        return typeKeyWithExtends.resolveWithTypeKeyInheritance(baseKey, translatedXmEntityTypeKey,
                                                                tk -> new String[] {
                                                                    translateToLepConvention(tk),
                                                                    translatedNextStateKey
                                                                });
    }

}
