package com.icthh.xm.ms.entity.lep.keyresolver;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;

import java.util.List;

/**
 * The {@link FunctionLepKeyResolver} class.
 */
public class FunctionLepKeyResolver implements LepKeyResolver {

    /**
     * Method parameter name for {@code functionKey}.
     */
    private static final String PARAM_FUNCTION_KEY = "functionKey";

    @Override
    public String group(LepMethod method) {
        String functionKey = method.getParameter(PARAM_FUNCTION_KEY, String.class);
        return functionKey.substring(0, functionKey.lastIndexOf('/'));
    }

    @Override
    public List<String> segments(LepMethod method) {
        String functionKey = method.getParameter(PARAM_FUNCTION_KEY, String.class);
        return List.of(functionKey.substring(functionKey.lastIndexOf('/') + 1));
    }
}
