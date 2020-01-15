package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The {@link FunctionContextServiceImpl} class.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class FunctionContextServiceImpl implements FunctionContextService {

    private final FunctionContextRepository functionContextRepository;

    private final PermittedRepository permittedRepository;

    private final PermittedSearchRepository permittedSearchRepository;

    private final StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private final XmEntityRepository xmEntityRepository;

    /**
     * Save a functionContext.
     *
     * @param functionContext the entity to save
     * @return the persisted entity
     */
    public FunctionContext save(FunctionContext functionContext) {

        startUpdateDateGenerationStrategy.preProcessStartUpdateDates(functionContext,
                                                                     functionContext.getId(),
                                                                     functionContextRepository,
                                                                     FunctionContext::setStartDate,
                                                                     FunctionContext::getStartDate,
                                                                     FunctionContext::setUpdateDate);
        if (functionContext.getXmEntity() != null) {
            functionContext.setXmEntity(xmEntityRepository.getOne(functionContext.getXmEntity().getId()));
        }
        return functionContextRepository.save(functionContext);
    }

    /**
     * Get all the functionContexts.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("FUNCTION_CONTEXT.GET_LIST")
    @PrivilegeDescription("Privilege to get all the functionContexts")
    public List<FunctionContext> findAll(String privilegeKey) {
        return permittedRepository.findAll(FunctionContext.class, privilegeKey);
    }

    /**
     * Get one functionContext by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public FunctionContext findOne(Long id) {
        return functionContextRepository.findById(id).orElse(null);
    }

    /**
     * Delete the  functionContext by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        functionContextRepository.deleteById(id);
    }

    /**
     * Search for the functionContext corresponding to the query.
     *
     * @param query the query of the search
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    @FindWithPermission("FUNCTION_CONTEXT.SEARCH")
    @PrivilegeDescription("Privilege to search for the functionContext corresponding to the query")
    public List<FunctionContext> search(String query, String privilegeKey) {
        return permittedSearchRepository.search(query, FunctionContext.class, privilegeKey);
    }

}
