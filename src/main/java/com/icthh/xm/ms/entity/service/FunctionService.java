package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;

import java.util.Map;

/**
 * Service Implementation for functions managing.
 */
public interface FunctionService {

    /**
     * Execute function with binding to XmEntity instance.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param idOrKey       XmEntity id or key
     * @param functionInput function input context
     * @return function execution result
     */
    FunctionContext execute(String functionKey,
                            IdOrKey idOrKey,
                            Map<String, Object> functionInput);

    /**
     * Execute function without binding to any XmEntity instance.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function execution result
     */
    FunctionContext execute(String functionKey, Map<String, Object> functionInput);

     /**
     * Execute anonymous function.
     *
     * @param functionKey   the function key, unique in Tenant
     * @param functionInput function input context
     * @return function execution result
     */
    FunctionContext executeAnonymous(String functionKey, Map<String, Object> functionInput);

}
