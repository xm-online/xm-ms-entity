package com.icthh.xm.ms.entity.service;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.simpleQueryStringQuery;

import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.commons.errors.exception.EntityNotFoundException;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.lep.LogicExtensionPoint;
import com.icthh.xm.ms.entity.lep.spring.LepService;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.api.XmEntityService;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Implementation for managing XmEntity.
 */
@Slf4j
@LepService(group = "service.entity", name = "default")
@Transactional
public class XmEntityServiceImpl implements XmEntityService {

    private static final String TYPE_KEY = "typeKey";

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityRepository xmEntityRepository;
    private final XmEntitySearchRepository xmEntitySearchRepository;
    private final XmEntityLifeCycleService lifeCycleService;
    private final XmFunctionExecutorService functionExecutorService;
    /**
     * XM function service required by function executor to save function result.
     */
    private final XmFunctionService xmFunctionService;

    public XmEntityServiceImpl(XmEntitySpecService xmEntitySpecService,
                               XmEntityRepository xmEntityRepository,
                               XmEntitySearchRepository xmEntitySearchRepository,
                               XmEntityLifeCycleService lifeCycleService,
                               XmFunctionExecutorService functionExecutorService,
                               @Qualifier("xmFunctionServiceResolver")
                               XmFunctionService xmFunctionService) {
        this.xmEntitySpecService = xmEntitySpecService;
        this.xmEntityRepository = xmEntityRepository;
        this.xmEntitySearchRepository = xmEntitySearchRepository;
        this.lifeCycleService = lifeCycleService;
        this.functionExecutorService = functionExecutorService;
        this.xmFunctionService = xmFunctionService;
    }

    /**
     * Save a xmEntity.
     *
     * @param xmEntity the entity to save
     * @return the persisted entity
     */
    @LogicExtensionPoint("Save")
    @Override
    public XmEntity save(XmEntity xmEntity) {
        log.debug("Request to save XmEntity : {}", xmEntity);

        // FIXME It is hack to link each tag with entity before persisting. may be there is more elegant solution.
        xmEntity.getTags().forEach(tag -> {
            if (tag.getXmEntity() == null) {
                tag.setXmEntity(xmEntity);
            }
        });
        xmEntity.getLocations().forEach(location -> {
            if (location.getXmEntity() == null) {
                location.setXmEntity(xmEntity);
            }
        });
        xmEntity.getAttachments().forEach(attachment -> {
            if (attachment.getXmEntity() == null) {
                attachment.setXmEntity(xmEntity);
            }
        });

        xmEntity.getTargets().forEach(target -> {
            if (target.getSource() == null) {
                target.setSource(xmEntity);
            }
        });

        xmEntity.getSources().forEach(source -> {
            if (source.getTarget() == null) {
                source.setTarget(xmEntity);
            }
        });

        XmEntity result = xmEntityRepository.save(xmEntity);
        xmEntitySearchRepository.save(result);
        return result;
    }

    /**
     * Get all the xmEntities.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @LogicExtensionPoint("FindAll")
    @Override
    @Transactional(readOnly = true)
    public Page<XmEntity> findAll(Pageable pageable, String typeKey) {
        log.debug("Request to get all XmEntities");
        if (StringUtils.isNoneBlank(typeKey)) {
            Set<String> typeKeys = xmEntitySpecService.findNonAbstractTypesByPrefix(typeKey).stream()
                .map(TypeSpec::getKey).collect(Collectors.toSet());
            log.debug("Find by typeKeys {}", typeKeys);
            return xmEntityRepository.findAllByTypeKeyIn(pageable, typeKeys);
        } else {
            return xmEntityRepository.findAll(pageable);
        }
    }

    /**
     * Get one xmEntity by id or key.
     *
     * @param idOrKey the id or key of the entity
     * @return the entity
     */
    @LogicExtensionPoint("FindOne")
    @Override
    @Transactional(readOnly = true)
    public XmEntity findOne(IdOrKey idOrKey) {
        log.debug("Request to get XmEntity : {}", idOrKey);
        if (idOrKey.isKey()) {
            throw new IllegalArgumentException("Key mode is not supported yet");
        }
        return xmEntityRepository.findOneById(idOrKey.getId());
    }

    /**
     * Delete the  xmEntity by id.
     *
     * @param id the id of the entity
     */
    @LogicExtensionPoint("Delete")
    @Override
    public void delete(Long id) {
        log.debug("Request to delete XmEntity : {}", id);
        xmEntityRepository.delete(id);
        xmEntitySearchRepository.delete(id);
    }

    /**
     * Search for the xmEntity corresponding to the query.
     *
     * @param query    the query of the search
     * @param pageable the pagination information
     * @return the list of entities
     */
    @LogicExtensionPoint("Search")
    @Override
    @Transactional(readOnly = true)
    public Page<XmEntity> search(String query, Pageable pageable) {
        log.debug("Request to search for a page of XmEntities for query {}", query);
        return xmEntitySearchRepository.search(queryStringQuery(query), pageable);
    }

    @LogicExtensionPoint("Register")
    @Override
    @Transactional(readOnly = true)
    public void register(XmEntity xmEntity) {
        throw new BusinessException("method 'register' is not supported");
    }

    @LogicExtensionPoint("Activate")
    @Override
    public void activate(String code) {
        throw new BusinessException("method 'activate' is not supported");
    }

    @LogicExtensionPoint("Profile")
    @Override
    @Transactional(readOnly = true)
    public XmEntity profile() {
        throw new BusinessException("method 'profile' is not supported");
    }

    @LogicExtensionPoint("GetLinkTargets")
    @Override
    public List<Link> getLinkTargets(IdOrKey idOrKey, String typeKey) {
        throw new BusinessException("method 'getLinkTargets' is not supported");
    }

    @LogicExtensionPoint("SaveLinkTarget")
    @Override
    public Link saveLinkTarget(IdOrKey idOrKey, Link link, MultipartFile file) {
        throw new BusinessException("method 'saveLinkTarget' is not supported");
    }

    @LogicExtensionPoint("UpdateLinkTarget")
    @Override
    public Link updateLinkTarget(IdOrKey idOrKey, String targetId, Link link, MultipartFile file) {
        throw new BusinessException("method 'updateLinkTarget' is not supported");
    }

    @LogicExtensionPoint("DeleteLinkTarget")
    @Override
    public void deleteLinkTarget(IdOrKey idOrKey, String linkKey) {
        throw new BusinessException("method 'deleteLinkTarget' is not supported");
    }

    @LogicExtensionPoint("GetSelfLinkTargets")
    @Override
    public List<Link> getSelfLinkTargets(String typeKey) {
        throw new BusinessException("method 'getSelfLinkTargets' is not supported");
    }

    @LogicExtensionPoint("SaveSelfLinkTarget")
    @Override
    public Link saveSelfLinkTarget(Link link, MultipartFile file) {
        throw new BusinessException("method 'saveSelfLinkTarget' is not supported");
    }

    @LogicExtensionPoint("UpdateSelfLinkTarget")
    @Override
    public Link updateSelfLinkTarget(String targetId, Link link, MultipartFile file) {
        throw new BusinessException("method 'updateSelfLinkTarget' is not supported");
    }

    @LogicExtensionPoint("DeleteSelfLinkTarget")
    @Override
    public void deleteSelfLinkTarget(String linkId) {
        throw new BusinessException("method 'deleteSelfLinkTarget' is not supported");
    }

    @LogicExtensionPoint("ExecuteFunction")
    @Override
    public XmFunction executeFunction(IdOrKey idOrKey, String functionKey, Map<String, Object> functionContext) {
        Objects.requireNonNull(idOrKey, "idOrKey can't be null");
        Objects.requireNonNull(functionKey, "functionKey can't be null");
        if (functionContext == null) {
            functionContext = Collections.emptyMap();
        }

        // find
        XmEntity xmEntity;
        if (idOrKey.isId()) {
            // ID case
            xmEntity = xmEntityRepository.findOneById(idOrKey.getId());
            if (xmEntity == null) {
                throw new EntityNotFoundException("XmEntity with id [" + idOrKey.getId() + "] not found");
            }
        } else {
            // KEY case
            throw new BusinessException("Method 'executeFunction' is not supported for 'key' case yet");
        }

        String typeKey = xmEntity.getTypeKey();
        Optional<FunctionSpec> functionSpec = xmEntitySpecService.findFunction(typeKey, functionKey);
        if (!functionSpec.isPresent()) {
            throw new IllegalArgumentException("Function not found, function key: " + functionKey);
        }

        // execute function
        Map<String, Object> functionData = functionExecutorService.executeFunction(idOrKey, typeKey, functionKey,
                                                                                   functionContext);
        XmFunction functionResult = new XmFunction();
        // TODO review keys ...
        functionResult.setTypeKey(functionKey);
        functionResult.setKey(functionKey + "-" + UUID.randomUUID().toString());

        functionResult.setXmEntity(xmEntity);
        functionResult.setData(functionData);
        functionResult.setStartDate(Instant.now());
        functionResult.setUpdateDate(functionResult.getStartDate());

        return xmFunctionService.save(functionResult);
    }

    @Deprecated
    @Override
    public void updateState(String id, XmEntity xmEntity) {
        throw new BusinessException("method 'updateState' is not supported");
    }

    @LogicExtensionPoint("UpdateState")
    @Override
    public XmEntity updateState(IdOrKey idOrKey, String stateKey, Map<String, Object> context) {
        XmEntity entity = findOne(idOrKey);
        if (xmEntitySpecService.nextStates(entity.getTypeKey(), entity.getStateKey()).stream()
                        .anyMatch(stateSpec -> stateKey.equals(stateSpec.getKey()))) {
            return lifeCycleService.changeState(idOrKey, stateKey, context);
        } else {
            throw new IllegalArgumentException("Entity " + entity + " can not go from ["
                            + entity.getStateKey() + "] to [" + stateKey + "]");
        }
    }

    @LogicExtensionPoint("SearchByQueryAndTypeKey")
    @Override
    @Transactional(readOnly = true)
    public Page<XmEntity> searchByQueryAndTypeKey(String query, String typeKey, Pageable pageable) {
        val prefix = (typeKey + ".").toLowerCase(); // TODO disable elastic analized to field "typeKey" and fix this

        val typeKeyQuery = boolQuery()
            .should(matchQuery(TYPE_KEY, typeKey))
            .should(prefixQuery(TYPE_KEY, prefix)).minimumNumberShouldMatch(1);

        val esQuery = isEmpty(query) ? boolQuery().must(typeKeyQuery) : typeKeyQuery.must(simpleQueryStringQuery(query));

        return xmEntitySearchRepository.search(esQuery, pageable);
    }

    @LogicExtensionPoint("UpdateAvatar")
    @Override
    public URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity) {
        throw new BusinessException("method 'updateAvatar' is not supported yet");
    }

    @LogicExtensionPoint("UpdateSelfAvatar")
    @Override
    public URI updateSelfAvatar(HttpEntity<Resource> avatarHttpEntity) {
        throw new BusinessException("method 'updateSelfAvatar' is not supported yet");
    }

    @LogicExtensionPoint("FindOneEmbed")
    @Override
    @Transactional(readOnly = true)
    public XmEntity findOne(IdOrKey idOrKey, List<String> embed) {
        if (idOrKey.isKey()) {
            throw new IllegalArgumentException("Key mode is not supported yet");
        }
        return xmEntityRepository.findOne(idOrKey.getId(), embed);
    }
}
