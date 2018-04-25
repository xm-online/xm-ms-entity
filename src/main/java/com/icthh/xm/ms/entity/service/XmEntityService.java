package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
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

    Page<XmEntity> findByIds(Pageable pageable, Set<Long> ids, List<String> embed, Object privilegeKey);

    List<XmEntity> findAll(Specification<XmEntity> spec);

    @LoggingAspectConfig(resultDetails = false)
    XmEntity findOne(IdOrKey idOrKey);

    @LoggingAspectConfig(resultDetails = false)
    XmEntity selectAndUpdate(IdOrKey idOrKey, Consumer<XmEntity> consumer);

    void delete(Long id);

    Page<XmEntity> search(String query, Pageable pageable, String privilegeKey);

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

    Link updateLinkTarget(IdOrKey idOrKey, String targetId, Link link, MultipartFile file);

    Link saveLinkTarget(IdOrKey idOrKey, Link link, MultipartFile file);

    void deleteLinkTarget(IdOrKey idOrKey, String linkId);

    XmEntity addFileAttachment(XmEntity entity, MultipartFile file);

    default Optional<XmEntityStateProjection> findStateProjectionById(IdOrKey idOrKey) {
        throw new NotImplementedException("Method findStateProjectionById not implemented");
    }

    Page<XmEntity> searchByQueryAndTypeKey(@Nullable String query, String typeKey, Pageable pageable, String privilegeKey);

    XmEntity findOne(IdOrKey idOrKey, List<String> embed);

    URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity);

    XmEntityIdKeyTypeKey getXmEntityIdKeyTypeKey(IdOrKey idOrKey);

    boolean existsByTypeKeyIgnoreCase(String typeKey, String name);

    @Override
    Object findById(Object id);

    byte[] exportEntities(String fileFormat, String typeKey);
}
