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
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplate;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateExecutor;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
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

    public JpqlTemplate template(String templateKey) {
        return jpqlTemplatesService.getTemplate(templateKey);
    }

    public Page<XmEntityDto> searchByEntityTemplate(JpqlTemplate template, Map<String, Object> rawParams,
                                                    Pageable pageable, String privilegeKey) {
        Map<String, Object> params = jpqlTemplatesService.coerce(template, rawParams);
        return xmEntityDbSearchService.searchByEntityTemplate(template, params, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }

    public JpqlTemplateExecutor.RawResult searchByRawTemplate(JpqlTemplate template, Map<String, Object> rawParams, Pageable pageable) {
        Map<String, Object> params = jpqlTemplatesService.coerce(template, rawParams);
        return xmEntityDbSearchService.searchByRawTemplate(template, params, pageable, value -> {
            if (value instanceof XmEntity xmEntity) {
                return xmEntityMapper.toDto(xmEntity);
            }
            if (value instanceof Link link) {
                return linkMapper.toDto(link);
            }
            return value;
        });
    }
}
