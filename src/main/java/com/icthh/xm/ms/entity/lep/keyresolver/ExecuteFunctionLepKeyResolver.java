package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.lep.api.LepKey;
import com.icthh.lep.api.LepManagerService;
import com.icthh.lep.api.LepMethod;
import com.icthh.lep.commons.GroupMode;
import com.icthh.lep.commons.GroupMode.Builder;
import com.icthh.lep.commons.GroupModeType;
import com.icthh.lep.commons.SeparatorSegmentedLepKey;
import com.icthh.xm.ms.entity.lep.SeparatorSegmentedLepKeyResolver;
import org.apache.commons.lang3.StringUtils;

import static com.icthh.xm.ms.entity.lep.XmLepConstants.SCRIPT_ANY_TYPE_KEY;

/**
 * The {@link ExecuteFunctionLepKeyResolver} class.
 */
public class ExecuteFunctionLepKeyResolver extends SeparatorSegmentedLepKeyResolver {

    /**
     * Method parameter name for {@code xmEntityTypeKey}.
     */
    private static final String PARAM_TYPE_KEY = "xmEntityTypeKey";

    /**
     * Method parameter name for {@code functionKey}.
     */
    private static final String PARAM_FUNCTION_KEY = "functionKey";


    // functions.key: ACCOUNT.EXTRACT-LINKEDIN-PROFILE
    // '.' -> '$'
    // '-' -> '_'
    //

    /**
     * Function extension key:
     * {@code Function.<translated-type-key>.<translated-function-key>}
     */
    @Override
    protected LepKey resolveKey(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        GroupModeType baseKeyGroupMode = baseKey.getGroupMode().getType();
        if (baseKeyGroupMode != GroupModeType.PREFIX_EXCLUDE_LAST_SEGMENTS && baseKeyGroupMode != GroupModeType.PREFIX) {
            throw new IllegalArgumentException("Base key unsupported group mode: " + baseKeyGroupMode);
        }

        // function key
        String translatedFuncKey = translateToLepConvention(getRequiredStrParam(method, PARAM_FUNCTION_KEY));

        // type key
        String typeKey = getStrParam(method, PARAM_TYPE_KEY);
        String translatedTypeKey = StringUtils.isBlank(typeKey)
            ? SCRIPT_ANY_TYPE_KEY : translateToLepConvention(typeKey);

        String[] appendSegments = new String[] {translatedTypeKey, translatedFuncKey};

        GroupMode groupMode = new Builder().prefixAndIdIncludeGroup(baseKey.getGroupSegmentsSize()).build();

        return baseKey.append(appendSegments, groupMode);
    }

}
