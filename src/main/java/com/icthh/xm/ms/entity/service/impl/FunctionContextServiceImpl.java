package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;

import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyResolver;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The {@link FunctionContextServiceImpl} class.
 */
@RequiredArgsConstructor
@Service
@LepService(group = "service.functionContext")
@Transactional
public class FunctionContextServiceImpl implements FunctionContextService {

    private final FunctionContextRepository functionContextRepository;

    private final PermittedRepository permittedRepository;

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

    @Override
    @Transactional(readOnly = true)
    @FindWithPermission("FUNCTION_CONTEXT.GET_LIST.BY_XM_ENTITY")
    @LogicExtensionPoint(value = "FindByXmEntity", resolver = TypeKeyResolver.class)
    @PrivilegeDescription("Privilege to search for the functionContext by xmEntity id")
    public Page<FunctionContext> findByXmEntity(Long id, String typeKey, Pageable pageable, String privilegeKey) {
        return permittedRepository.findByCondition("returnObject.xmEntity.id = :id", of("id", id), pageable, FunctionContext.class, privilegeKey);
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

}
