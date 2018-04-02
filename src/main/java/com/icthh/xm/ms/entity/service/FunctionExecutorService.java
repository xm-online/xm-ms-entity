package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionLepKeyResolver;
import com.icthh.xm.ms.entity.lep.keyresolver.FunctionWithXmEntityLepKeyResolver;

import java.util.Map;

/**
 * The {@link FunctionExecutorService} interface.
 */
@LepService(group = "function")
public interface FunctionExecutorService {

    /**
     * Execute function with binding to XmEntity instance.
     *
     * @param functionKey     the function key, unique in Tenant
     * @param idOrKey         XmEntity id or key (can be {@code null})
     * @param xmEntityTypeKey XmEntity type key (can be {@code null})
     * @param functionInput   function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "FunctionWithXmEntity", resolver = FunctionWithXmEntityLepKeyResolver.class)
    Map<String, Object> execute(String functionKey,
                                IdOrKey idOrKey,
                                String xmEntityTypeKey,
                                Map<String, Object> functionInput);

    /**
     * Execute function without binding to any XmEntity instance.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function result data
     */
    @LogicExtensionPoint(value = "Function", resolver = FunctionLepKeyResolver.class)
    Map<String, Object> execute(String functionKey, Map<String, Object> functionInput);

}
