package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.XmFunction;

import java.util.List;
import java.util.Map;

/**
 * XmFunction service.
 */
public interface XmFunctionService {

    /**
     * Save a xmFunction.
     *
     * @param xmFunction the entity to save
     * @return the persisted entity
     */
    XmFunction save(XmFunction xmFunction);

    /**
     * Get all the xmFunctions.
     *
     * @return the list of entities
     */
    List<XmFunction> findAll();

    /**
     * Get one xmFunction by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    XmFunction findOne(Long id);

    /**
     * Delete the  xmFunction by id.
     *
     * @param id the id of the entity
     */
    void delete(Long id);

    /**
     * Search for the xmFunction corresponding to the query.
     *
     * @param query the query of the search
     * @return the list of entities
     */
    List<XmFunction> search(String query);

    /**
     * Execute function without binding to any XmEntity instance.
     *
     * @param functionKey the function key, unique in Tenant
     * @param context     function input context
     * @return function execution result
     */
    XmFunction execute(String functionKey, Map<String, Object> context);

}
