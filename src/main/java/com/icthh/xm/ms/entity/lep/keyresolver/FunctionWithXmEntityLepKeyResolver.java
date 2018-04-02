package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;

/**
 * The {@link FunctionWithXmEntityLepKeyResolver} class.
 */
public class FunctionWithXmEntityLepKeyResolver extends AppendLepKeyResolver {

    /**
     * Method parameter name for {@code functionKey}.
     */
    private static final String PARAM_FUNCTION_KEY = "functionKey";

    /**
     * Function extension key:
     * {@code Function.<translated-function-key>}
     * functions.key: ACCOUNT.EXTRACT-LINKEDIN-PROFILE
     * '.' -> '$'
     * '-' -> '_'
     * Script name segment: ACCOUNT$EXTRACT_LINKEDIN_PROFILE
     *
     * @param baseKey        not {@code null} base LEP extension key of type {@link SeparatorSegmentedLepKey}
     * @param method         LEP method
     * @param managerService manager service
     * @return dynamic value of LEP base key
     */
    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        // function key
        String translatedFuncKey = translateToLepConvention(getRequiredStrParam(method, PARAM_FUNCTION_KEY));

        return new String[] {
            translatedFuncKey
        };
    }

}
