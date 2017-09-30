package com.icthh.xm.ms.entity.service.api;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface XmEntityService {

    XmEntity save(XmEntity xmEntity);

    Page<XmEntity> findAll(Pageable pageable, String typeGroup);

    XmEntity findOne(IdOrKey idOrKey);

    void delete(Long id);

    Page<XmEntity> search(String query, Pageable pageable);

    void register(XmEntity xmEntity);

    void activate(String code);

    XmEntity profile();

    XmFunction executeFunction(IdOrKey idOrKey, String functionKey, Map<String, Object> functionContext);

    /**
     * @deprecated use {@link #updateState(IdOrKey, java.lang.String, java.util.Map)} instead
     */
    @Deprecated
    void updateState(String idOrKey, XmEntity xmEntity);

    XmEntity updateState(IdOrKey idOrKey,
                         String stateKey,
                         Map<String, Object> context);

    List<Link> getLinkTargets(IdOrKey idOrKey, String typeKey);

    Link saveLinkTarget(IdOrKey idOrKey, Link link, MultipartFile file);

    Link updateLinkTarget(IdOrKey idOrKey, String targetId, Link link, MultipartFile file);

    void deleteLinkTarget(IdOrKey idOrKey, String linkId);

    List<Link> getSelfLinkTargets(String typeKey);

    Link saveSelfLinkTarget(Link link, MultipartFile file);

    Link updateSelfLinkTarget(String targetId, Link link, MultipartFile file);

    void deleteSelfLinkTarget(String linkId);

    Page<XmEntity> searchByQueryAndTypeKey(@Nullable String query, String typeKey, Pageable pageable);

    XmEntity findOne(IdOrKey idOrKey, List<String> embed);

    URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity);

    URI updateSelfAvatar(HttpEntity<Resource> avatarHttpEntity);

}
