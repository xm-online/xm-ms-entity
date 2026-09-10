package com.icthh.xm.ms.entity.service.search.db;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Single place that recomputes {@code xm_entity.search_text}, used by the JPA listener and by reindex. */
@Component
@RequiredArgsConstructor
public class SearchTextUpdater {

    private final XmEntitySpecService xmEntitySpecService;
    private final SearchTextBuilder searchTextBuilder;

    public void refresh(XmEntity entity) {
        TypeSpec spec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(entity.getTypeKey()).orElse(null);
        entity.setSearchText(searchTextBuilder.build(spec, entity));
    }
}
