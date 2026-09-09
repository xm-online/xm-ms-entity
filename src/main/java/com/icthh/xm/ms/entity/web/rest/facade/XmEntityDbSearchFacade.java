package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.mapper.LinkMapper;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import com.icthh.xm.ms.entity.service.search.db.XmEntityDbSearchService;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
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
}
