package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.search.db.SearchTextBuilder;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Keeps {@code xm_entity.search_text} in sync with name, description and the data fields configured in the type spec. */
@Slf4j
@Component
public class XmEntitySearchTextListener {

    private final XmEntitySpecService xmEntitySpecService;
    private final SearchTextBuilder searchTextBuilder;

    /**
     * {@code @Lazy} on the spec service is required: entity listeners are instantiated while the
     * EntityManagerFactory is being built, and the spec service transitively depends on JPA repositories.
     */
    public XmEntitySearchTextListener(@Lazy XmEntitySpecService xmEntitySpecService,
                                      SearchTextBuilder searchTextBuilder) {
        this.xmEntitySpecService = xmEntitySpecService;
        this.searchTextBuilder = searchTextBuilder;
    }

    @PrePersist
    @PreUpdate
    void onPrePersistOrUpdate(XmEntity entity) {
        TypeSpec spec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(entity.getTypeKey()).orElse(null);
        entity.setSearchText(searchTextBuilder.build(spec, entity));
    }
}
