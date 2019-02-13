package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
// TODO: 14-Jan-19 Use commons ResourceRepository after fix
//import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.template.TemplateParamsHolder;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.ResourceRepository;
import com.icthh.xm.ms.entity.service.dto.LinkSourceDto;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public interface XmEntityService extends ResourceRepository {

    XmEntity save(XmEntity xmEntity);

    Page<XmEntity> findAll(Pageable pageable, String typeGroup, String privilegeKey);

    Page<XmEntity> findByIds(Pageable pageable, Set<Long> ids, Set<String> embed, String privilegeKey);

    List<XmEntity> findAll(Specification<XmEntity> spec);

    /***
     *
     * Only for lep usage
     *
     * @param jpql
     * @param args
     * @return
     */
    @Transactional(readOnly = true)
    List<XmEntity> findAll(String jpql, Map<String, Object> args, List<String> embed);

    @LoggingAspectConfig(resultDetails = false)
    XmEntity findOne(IdOrKey idOrKey);

    @LoggingAspectConfig(resultDetails = false)
    XmEntity selectAndUpdate(IdOrKey idOrKey, Consumer<XmEntity> consumer);

    void delete(Long id);

    Page<XmEntity> search(String query, Pageable pageable, String privilegeKey);

    Page<XmEntity> search(String template, TemplateParamsHolder templateParamsHolder, Pageable pageable, String privilegeKey);

    @Deprecated
    XmEntity profile();

    /**
     * @deprecated use {@link #updateState(IdOrKey, java.lang.String, java.util.Map)} instead
     */
    @Deprecated
    void updateState(String idOrKey, XmEntity xmEntity);

    XmEntity updateState(IdOrKey idOrKey,
                         String stateKey,
                         Map<String, Object> context);

    List<Link> getLinkTargets(IdOrKey idOrKey, String typeKey);

    List<Link> getLinkSources(IdOrKey idOrKey, String typeKey);

    Page<LinkSourceDto> getLinkSourcesInverted(Pageable pageable, IdOrKey idOrKey, Set<String> typeKey,
                                               String privilegeKey);

    Link updateLinkTarget(IdOrKey idOrKey, String targetId, Link link, MultipartFile file);

    Link saveLinkTarget(IdOrKey idOrKey, Link link, MultipartFile file);

    void deleteLinkTarget(IdOrKey idOrKey, String linkId);

    XmEntity addFileAttachment(XmEntity entity, MultipartFile file);

    void assertStateTransition(String stateKey, XmEntityStateProjection entity);

    default Optional<XmEntityStateProjection> findStateProjectionById(IdOrKey idOrKey) {
        throw new NotImplementedException("Method findStateProjectionById not implemented");
    }

    Page<XmEntity> searchByQueryAndTypeKey(@Nullable String query, String typeKey, Pageable pageable, String privilegeKey);

    Page<XmEntity> searchByQueryAndTypeKey(String template, TemplateParamsHolder templateParamsHolder, String typeKey, Pageable pageable, String privilegeKey);

    XmEntity findOne(IdOrKey idOrKey, List<String> embed);

    URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity);

    XmEntityIdKeyTypeKey getXmEntityIdKeyTypeKey(IdOrKey idOrKey);

    boolean existsByTypeKeyIgnoreCase(String typeKey, String name);

    @Override
    Object findResourceById(Object id);

    /**
     * For backward compatibility in LEPs.
     *
     * Deprecated: use findOne(IdOrKey idOrKey) instead.
     */
    @Deprecated
    XmEntity findById(Object id);

    byte[] exportEntities(String fileFormat, String typeKey);
}
