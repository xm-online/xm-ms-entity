package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.service.impl.FunctionExecutorServiceImpl;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import java.util.Map;

/**
 * The {@link XmEntityFunctionExecutorService} interface.
 */
@IgnoreLogginAspect
@LepService(group = "function")
public class XmEntityFunctionExecutorService extends FunctionExecutorServiceImpl {

    /**
     * Execute function with binding to XmEntity instance.
     *
     * @param functionKey     the function key, unique in Tenant
     * @param idOrKey         XmEntity id or key (can be {@code null})
     * @param xmEntityTypeKey XmEntity type key (can be {@code null})
     * @param functionInput   function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "FunctionWithXmEntity", resolver = FunctionLepKeyResolver.class)
    public Map<String, Object> execute(String functionKey,
                                IdOrKey idOrKey,
                                String xmEntityTypeKey,
                                Map<String, Object> functionInput) {
        throw new IllegalStateException("FunctionWithXmEntity " + functionKey + " not found");
    }
}
