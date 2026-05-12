package com.icthh.xm.ms.entity.domain.idresolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractXmEntityObjectIdResolver extends SimpleObjectIdResolver {
    protected Map<ObjectIdGenerator.IdKey, Object> items;

    protected XmEntityRepository repository;

    protected AbstractXmEntityObjectIdResolver(XmEntityRepository repository) {
        this.repository = repository;
    }

    protected AbstractXmEntityObjectIdResolver() {
        log.debug("{} inited", this.getClass().getSimpleName());
    }

    @Override
    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }

    @Override
    public void bindItem(ObjectIdGenerator.IdKey id, Object pojo) {
        if (items == null) {
            items = new HashMap<>();
        }

        Object existing = items.get(id);

        if (existing == null) {
            items.put(id, pojo);
        } else {
            // simulate Jackson 2-style - ignore duplicate
            if (existing != pojo) {
                log.debug("Duplicate object id detected for id {}. Ignoring new instance.", id.key);
            }
        }
    }
}
