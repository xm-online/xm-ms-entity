package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.FunctionContext;

import java.util.List;

/**
 * Service Implementation for managing FunctionContext.
 */
public interface FunctionContextService {

    /**
     * Save a functionContext.
     *
     * @param functionContext the entity to save
     * @return the persisted entity
     */
    FunctionContext save(FunctionContext functionContext);

    /**
     * Get all the functionContexts.
     *
     * @param privilegeKey the privilege key
     * @return the list of entities
     */
    List<FunctionContext> findAll(String privilegeKey);

    /**
     * Get one functionContext by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    FunctionContext findOne(Long id);

    /**
     * Delete the  functionContext by id.
     *
     * @param id the id of the entity
     */
    void delete(Long id);

    /**
     * Search for the functionContext corresponding to the query.
     *
     * @param query the query of the search
     * @param privilegeKey the privilege key
     * @return the list of entities
     */
    List<FunctionContext> search(String query, String privilegeKey);

}
