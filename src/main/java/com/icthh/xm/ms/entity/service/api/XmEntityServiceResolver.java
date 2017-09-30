package com.icthh.xm.ms.entity.service.api;

import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.annotation.Tenant;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class XmEntityServiceResolver implements XmEntityService {

    private final XmEntityService defaultService;

    public XmEntityServiceResolver(@Qualifier("default") XmEntityService defaultService) {
        this.defaultService = defaultService;
    }

    XmEntityService getService() {
        String tenant = TenantContext.getCurrent().getTenant();
        return defaultService;
    }

    @Override
    public XmEntity save(XmEntity xmEntity) {
        return getService().save(xmEntity);
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable, String typeGroup) {
        return getService().findAll(pageable, typeGroup);
    }

    @Override
    public XmEntity findOne(IdOrKey idOrKey) {
        return getService().findOne(idOrKey);
    }

    @Override
    public void delete(Long id) {
        getService().delete(id);
    }

    @Override
    public Page<XmEntity> search(String query, Pageable pageable) {
        return getService().search(query, pageable);
    }

    @Override
    public void register(XmEntity xmEntity) {
        getService().register(xmEntity);
    }

    @Override
    public void activate(String code) {
        getService().activate(code);
    }

    @Override
    public XmEntity profile() {
        return getService().profile();
    }

    @Override
    public List<Link> getLinkTargets(IdOrKey idOrKey, String typeKey) {
        return getService().getLinkTargets(idOrKey, typeKey);
    }

    @Override
    public Link saveLinkTarget(IdOrKey idOrKey, Link link, MultipartFile file) {
        return getService().saveLinkTarget(idOrKey, link, file);
    }

    @Override
    public Link updateLinkTarget(IdOrKey idOrKey, String targetId, Link link, MultipartFile file) {
        return getService().updateLinkTarget(idOrKey, targetId, link, file);
    }

    @Override
    public void deleteLinkTarget(IdOrKey idOrKey, String linkId) {
        getService().deleteLinkTarget(idOrKey, linkId);
    }


    @Override
    public List<Link> getSelfLinkTargets(String typeKey) {
        return getService().getSelfLinkTargets(typeKey);
    }

    @Override
    public Link saveSelfLinkTarget(Link link, MultipartFile file) {
        return getService().saveSelfLinkTarget(link, file);
    }

    @Override
    public Link updateSelfLinkTarget(String targetId, Link link, MultipartFile file) {
        return getService().updateSelfLinkTarget(targetId, link, file);
    }

    @Override
    public void deleteSelfLinkTarget(String linkId) {
        getService().deleteSelfLinkTarget(linkId);
    }

    @Override
    public Page<XmEntity> searchByQueryAndTypeKey(String query, String typeKey, Pageable pageable) {
        return getService().searchByQueryAndTypeKey(query, typeKey, pageable);
    }

    @Override
    public URI updateAvatar(IdOrKey idOrKey, HttpEntity<Resource> avatarHttpEntity) {
        return getService().updateAvatar(idOrKey, avatarHttpEntity);
    }

    @Override
    public URI updateSelfAvatar(HttpEntity<Resource> avatarHttpEntity) {
        return getService().updateSelfAvatar(avatarHttpEntity);
    }

    @Override
    public XmEntity findOne(IdOrKey idOrKey, List<String> embed) {
        return getService().findOne(idOrKey, embed);
    }

    @Override
    public XmFunction executeFunction(IdOrKey idOrKey, String functionKey, Map<String, Object> functionData) {
        return getService().executeFunction(idOrKey, functionKey, functionData);
    }

    /**
     * @deprecated use {@link #updateState(IdOrKey, java.lang.String, java.util.Map)} instead
     */
    @Deprecated
    @Override
    public void updateState(String idOrKey, XmEntity xmEntity) {
        getService().updateState(idOrKey, xmEntity);
    }

    @Override
    public XmEntity updateState(IdOrKey idOrKey, String stateKey, Map<String, Object> context) {
        return getService().updateState(idOrKey, stateKey, context);
    }

}
