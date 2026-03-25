package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.template.TemplateParamsHolder;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.LinkSourceDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.LinkMapper;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class XmEntityFacade {

    private final XmEntityService xmEntityService;
    private final XmEntityMapper xmEntityMapper;
    private final LinkMapper linkMapper;

    public XmEntityDto save(XmEntityDto dto) {
        XmEntity entity = xmEntityMapper.toEntity(dto);
        XmEntity saved = xmEntityService.save(entity);
        return xmEntityMapper.toDto(saved);
    }

    public Page<XmEntityDto> findAll(Pageable pageable, String typeKey, String privilegeKey) {
        return xmEntityService.findAll(pageable, typeKey, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> findByIds(Pageable pageable, Set<Long> ids, Set<String> embed, String privilegeKey) {
        return xmEntityService.findByIds(pageable, ids, embed, privilegeKey).map(xmEntityMapper::toDto);
    }

    public XmEntityDto findOne(IdOrKey idOrKey) {
        XmEntity entity = xmEntityService.findOne(idOrKey);
        return entity != null ? xmEntityMapper.toDto(entity) : null;
    }

    public XmEntityDto findOne(IdOrKey idOrKey, List<String> embed) {
        XmEntity entity = xmEntityService.findOne(idOrKey, embed);
        return entity != null ? xmEntityMapper.toDto(entity) : null;
    }

    public void delete(Long id) {
        xmEntityService.delete(id);
    }

    public XmEntityDto profile() {
        XmEntity entity = xmEntityService.profile();
        return entity != null ? xmEntityMapper.toDto(entity) : null;
    }

    public boolean existsByTypeKeyIgnoreCase(String typeKey, String name) {
        return xmEntityService.existsByTypeKeyIgnoreCase(typeKey, name);
    }

    public List<LinkDto> getLinkTargets(IdOrKey idOrKey, String typeKey) {
        List<Link> links = xmEntityService.getLinkTargets(idOrKey, typeKey);
        return linkMapper.toDtoList(links);
    }

    public LinkDto saveLinkTarget(IdOrKey idOrKey, LinkDto linkDto, MultipartFile file) {
        Link link = linkMapper.toEntity(linkDto);
        Link saved = xmEntityService.saveLinkTarget(idOrKey, link, file);
        return linkMapper.toDto(saved);
    }

    public LinkDto updateLinkTarget(IdOrKey idOrKey, String targetId, LinkDto linkDto, MultipartFile file) {
        Link link = linkMapper.toEntity(linkDto);
        Link saved = xmEntityService.updateLinkTarget(idOrKey, targetId, link, file);
        return linkMapper.toDto(saved);
    }

    public void deleteLinkTarget(IdOrKey idOrKey, String targetId) {
        xmEntityService.deleteLinkTarget(idOrKey, targetId);
    }

    public List<LinkDto> getLinkSources(IdOrKey idOrKey, String typeKey) {
        List<Link> links = xmEntityService.getLinkSources(idOrKey, typeKey);
        return linkMapper.toDtoList(links);
    }

    public Page<LinkSourceDto> getLinkSourcesInverted(Pageable pageable, IdOrKey idOrKey, Set<String> typeKeys, String privilegeKey) {
        return xmEntityService.getLinkSourcesInverted(pageable, idOrKey, typeKeys, privilegeKey);
    }

    public void updateState(String id, XmEntityDto dto) {
        XmEntity entity = xmEntityMapper.toEntity(dto);
        xmEntityService.updateState(id, entity);
    }

    public XmEntityDto updateState(IdOrKey idOrKey, String stateKey, Map<String, Object> context) {
        XmEntity entity = xmEntityService.updateState(idOrKey, stateKey, context);
        return xmEntityMapper.toDto(entity);
    }

    public byte[] exportEntities(String fileFormat, String typeKey) throws IOException {
        return xmEntityService.exportEntities(fileFormat, typeKey);
    }

    public Page<XmEntityDto> search(String query, Pageable pageable, String privilegeKey) {
        return xmEntityService.search(query, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> search(String template, TemplateParamsHolder templateParamsHolder, Pageable pageable, String privilegeKey) {
        return xmEntityService.search(template, templateParamsHolder, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> searchByQueryAndTypeKey(String query, String typeKey, Pageable pageable, String privilegeKey) {
        return xmEntityService.searchByQueryAndTypeKey(query, typeKey, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> searchByQueryAndTypeKey(String template, TemplateParamsHolder templateParamsHolder, String typeKey, Pageable pageable, String privilegeKey) {
        return xmEntityService.searchByQueryAndTypeKey(template, templateParamsHolder, typeKey, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> searchXmEntitiesToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey, String query, Pageable pageable, String privilegeKey) {
        return xmEntityService.searchXmEntitiesToLink(idOrKey, entityTypeKey, linkTypeKey, query, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> searchV2(SearchDto searchDto, String privilegeKey) {
        return xmEntityService.searchV2(searchDto, privilegeKey).map(xmEntityMapper::toDto);
    }
}
