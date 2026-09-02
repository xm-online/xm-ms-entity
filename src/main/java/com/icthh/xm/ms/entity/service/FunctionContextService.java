package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Get the functionContexts of a specific xmEntity.
     *
     * @param id the id of the xmEntity
     * @param typeKey the typeKey of the xmEntity
     * @param pageable the pagination information
     * @param privilegeKey the privilege key
     * @return the page of entities
     */
    Page<FunctionContext> findByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey);

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

}
