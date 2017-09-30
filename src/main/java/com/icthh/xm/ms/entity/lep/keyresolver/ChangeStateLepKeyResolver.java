package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.lep.api.LepKey;
import com.icthh.lep.api.LepManagerService;
import com.icthh.lep.api.LepMethod;
import com.icthh.lep.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.lep.SeparatorSegmentedLepKeyResolver;

import static com.icthh.xm.ms.entity.lep.XmLepConstants.SCRIPT_NAME_SEPARATOR;

/**
 * The {@link ChangeStateLepKeyResolver} class.
 */
public class ChangeStateLepKeyResolver extends SeparatorSegmentedLepKeyResolver {

    /**
     * Method parameter name for {@code stateKey}.
     */
    private static final String PARAM_NEXT_STATE_KEY = "nextStateKey";

    /**
     * Method parameter name for {@code xmEntityTypeKey}.
     */
    private static final String PARAM_TYPE_KEY = "xmEntityTypeKey";

    /**
     * LEP extension key specification:<br>
     * {@code ChangeState$$<xm-entity-type-key>$$<next-state-key>}
     * <p>
     * Add to method name {@code <xm-entity-type-key} and {@code <state-key>} to the end.
     *
     * @param baseKey        base LEP key (prefix), can be {@code null}
     * @param method         method data on what LEP call occurs
     * @param managerService LEP manager service
     * @return complete LEP key (baseKey + dynamic part)
     */
    @Override
    protected LepKey resolveKey(SeparatorSegmentedLepKey baseKey,
                                LepMethod method,
                                LepManagerService managerService) {
        String nextStateKey = getRequiredStrParam(method, PARAM_NEXT_STATE_KEY);
        String xmEntityTypeKey = getRequiredStrParam(method, PARAM_TYPE_KEY);

        String typeNamePart = translateToLepConvention(xmEntityTypeKey);

        String[] segments = baseKey.getSegments();

        // add state key name to the end
        int scriptNameSegmentIndex = segments.length - 1;
        segments[scriptNameSegmentIndex] += SCRIPT_NAME_SEPARATOR + typeNamePart + SCRIPT_NAME_SEPARATOR + nextStateKey;

        return new SeparatorSegmentedLepKey(baseKey.getSeparator(),
                                            segments,
                                            baseKey.getGroupMode());
    }

}
