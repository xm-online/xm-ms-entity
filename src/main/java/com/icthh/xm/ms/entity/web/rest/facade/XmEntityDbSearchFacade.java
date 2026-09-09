package com.icthh.xm.ms.entity.web.rest.facade;

import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
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

    public Page<XmEntityDto> search(XmEntityDbSearchRequest request, Pageable pageable, String privilegeKey) {
        return xmEntityDbSearchService.search(request, pageable, privilegeKey).map(xmEntityMapper::toDto);
    }
}
