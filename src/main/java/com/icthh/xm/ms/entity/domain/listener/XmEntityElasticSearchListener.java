package com.icthh.xm.ms.entity.domain.listener;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;

@Slf4j
@Component
public class XmEntityElasticSearchListener {

    private static XmEntitySearchRepository xmEntitySearchRepository;

    private static XmEntitySpecService xmEntitySpecService;

    @Autowired
    public void setXmEntitySearchRepository(XmEntitySearchRepository xmEntitySearchRepository) {
        this.xmEntitySearchRepository = xmEntitySearchRepository;
    }

    @Autowired
    public void setXmEntitySpecService(XmEntitySpecService xmEntitySpecService) {
        this.xmEntitySpecService = xmEntitySpecService;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Listener for XmEntity [{}, {}]", xmEntitySearchRepository, xmEntitySpecService);
    }

    @PostPersist
    @PostUpdate
    void onPostPersistOrUpdate(XmEntity entity) {
        if (isFeatureEnabled(entity, TypeSpec::getIndexAfterSaveEnabled)) {
            log.debug("Save xm entity to elastic {}", entity);
            xmEntitySearchRepository.save(entity);
        }
    }

    @PostRemove
    void onPostRemove(XmEntity entity) {
        if(isFeatureEnabled(entity, TypeSpec::getIndexAfterDeleteEnabled)){
            log.debug("Delete xm entity from elastic {}", entity);
            xmEntitySearchRepository.delete(entity);
        }
    }

    private boolean isFeatureEnabled(XmEntity entity, Function<TypeSpec, Boolean> flag) {
        return xmEntitySpecService.getTypeSpecByKey(entity.getTypeKey())
                                  .map(flag)
                                  .orElse(false);
    }
}
