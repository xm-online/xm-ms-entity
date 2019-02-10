package com.icthh.xm.ms.entity.lep.keyresolver;

import static com.icthh.xm.commons.lep.XmLepConstants.EXTENSION_KEY_GROUP_MODE;
import static com.icthh.xm.commons.lep.XmLepConstants.EXTENSION_KEY_SEPARATOR;

import com.icthh.xm.commons.lep.SeparatorSegmentedLepKeyResolver;
import com.icthh.xm.commons.lep.XmLepConstants;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.GroupMode;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;

/**
 * The {@link FunctionLepKeyResolver} class.
 */
public class FunctionLepKeyResolver extends SeparatorSegmentedLepKeyResolver {

    /**
     * Method parameter name for {@code functionKey}.
     */
    private static final String PARAM_FUNCTION_KEY = "functionKey";

    @Override
    protected LepKey resolveKey(SeparatorSegmentedLepKey inBaseKey, LepMethod method, LepManagerService managerService) {
        String functionKey = getRequiredParam(method, PARAM_FUNCTION_KEY, String.class);
        int index = functionKey.lastIndexOf("/") + 1;

        String name = functionKey.substring(index);
        String separator = inBaseKey.getSeparator();
        String pathToFunction = functionKey.substring(0, index);
        String group = inBaseKey.getGroupKey().getId() + separator + pathToFunction.replaceAll("/", separator) +
                       inBaseKey.getSegments()[inBaseKey.getGroupSegmentsSize()];

        SeparatorSegmentedLepKey baseKey = new SeparatorSegmentedLepKey(group, EXTENSION_KEY_SEPARATOR, EXTENSION_KEY_GROUP_MODE);
        GroupMode groupMode = new GroupMode.Builder().prefixAndIdIncludeGroup(baseKey.getGroupSegmentsSize()).build();
        return baseKey.append(name, groupMode);
    }

}
