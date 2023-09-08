package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionLepKeyResolver;
import java.util.Map;

/**
 * The {@link FunctionExecutorService} interface.
 */
@IgnoreLogginAspect
@LepService(group = "function")
public class FunctionExecutorService {

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

    /**
     * Execute function without binding to any XmEntity instance.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "Function", resolver = FunctionLepKeyResolver.class)
    public Map<String, Object> execute(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        throw new IllegalStateException("Function " + functionKey + " not found");
    }

    /**
     * Execute anonymous function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "AnonymousFunction", resolver = FunctionLepKeyResolver.class)
    public Map<String, Object> executeAnonymousFunction(String functionKey, Map<String, Object> functionInput, String httpMethod) {
        throw new IllegalStateException("AnonymousFunction " + functionKey + " not found");
    }

}
