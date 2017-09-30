package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.LogicExtensionPoint;
import com.icthh.xm.ms.entity.lep.keyresolver.ExecuteFunctionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.spring.LepService;

import java.util.Map;

/**
 * The {@link XmFunctionExecutorService} interface.
 */
@LepService(group = "xm.entity.function")
public interface XmFunctionExecutorService {

    @LogicExtensionPoint(value = "Function", resolver = ExecuteFunctionLepKeyResolver.class)
    Map<String, Object> executeFunction(IdOrKey idOrKey,
                                        String xmEntityTypeKey,
                                        String functionKey,
                                        Map<String, Object> functionContext);

}
