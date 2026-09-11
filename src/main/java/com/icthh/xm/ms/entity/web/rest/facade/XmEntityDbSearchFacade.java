package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.LinkMapper;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import com.icthh.xm.ms.entity.service.search.db.XmEntityDbSearchService;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateType;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateExecutor;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Maps DB search results to DTOs. Read-only transactional so lazy associations are still open while mapping. */
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class XmEntityDbSearchFacade {

    private final XmEntityDbSearchService xmEntityDbSearchService;
    private final XmEntityMapper xmEntityMapper;
    private final LinkMapper linkMapper;
    private final XmEntityJpqlTemplatesService jpqlTemplatesService;

    public Page<XmEntityDto> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.search(request, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public Page<XmEntityDto> searchToLink(IdOrKey idOrKey, String entityTypeKey, String linkTypeKey,
                                          XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.searchToLink(idOrKey, entityTypeKey, linkTypeKey, request, pageable, privilegeKey)
            .map(xmEntityMapper::toDto);
    }

    public Page<LinkDto> searchTargets(IdOrKey idOrKey, String linkTypeKey, XmEntityDbSearchRequest request,
                                       Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.searchTargets(idOrKey, linkTypeKey, request, pageable, privilegeKey).map(linkMapper::toDto);
    }

    public JpqlTemplateType templateType(String templateKey) {
        return jpqlTemplatesService.getTemplate(templateKey).getType();
    }

    public Page<XmEntityDto> searchByEntityTemplate(String templateKey, Map<String, Object> requestParams, Pageable pageable) {
        return xmEntityDbSearchService.searchByEntityTemplate(templateKey, requestParams, pageable).map(xmEntityMapper::toDto);
    }

    public JpqlTemplateExecutor.RawResult searchByRawTemplate(String templateKey, Map<String, Object> requestParams,
                                                              Pageable pageable) {
        return xmEntityDbSearchService.searchByRawTemplate(templateKey, requestParams, pageable, value -> {
            if (value instanceof XmEntity xmEntity) {
                return xmEntityMapper.toDto(xmEntity);
            }
            if (value instanceof Link link) {
                return linkMapper.toDto(link);
            }
            // RAW templates may select anything: ids, field pairs, any entity of the service
            return Hibernate.unproxy(value);
        });
    }
}
