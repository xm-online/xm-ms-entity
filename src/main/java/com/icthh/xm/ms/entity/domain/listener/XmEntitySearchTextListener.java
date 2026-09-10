package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.db.SearchTextUpdater;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Keeps {@code xm_entity.search_text} in sync with name, description and the data fields configured in the type spec. */
@Slf4j
@Component
public class XmEntitySearchTextListener {

    private final SearchTextUpdater searchTextUpdater;

    /**
     * The updater is injected {@code @Lazy} (and therefore by an explicit constructor, not Lombok): entity
     * listeners are created while the EntityManagerFactory is being built, and the updater transitively
     * depends on JPA repositories.
     */
    public XmEntitySearchTextListener(@Lazy SearchTextUpdater searchTextUpdater) {
        this.searchTextUpdater = searchTextUpdater;
    }

    @PrePersist
    @PreUpdate
    void onPrePersistOrUpdate(XmEntity entity) {
        searchTextUpdater.refresh(entity);
    }
}
