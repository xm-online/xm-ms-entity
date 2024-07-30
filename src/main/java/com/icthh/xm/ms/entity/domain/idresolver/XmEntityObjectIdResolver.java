package com.icthh.xm.ms.entity.domain.idresolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * XmEntity object ID resolver.
 * Resolves XmEntity object from database bi ID provided in JSON.
 * see https://stackoverflow.com/questions/41989906/jackson-referencing-an-object-as-a-property
 */
@Slf4j
@Component
@Scope("prototype")
public class XmEntityObjectIdResolver implements ObjectIdResolver {
    protected Map<ObjectIdGenerator.IdKey,Object> items;

    private XmEntityRepository repository;

    @Autowired
    public XmEntityObjectIdResolver(XmEntityRepository repository) {
        this.repository = repository;
    }

    public XmEntityObjectIdResolver() {
        log.debug("XmEntity object id resolver inited");
    }

    @Override
    public void bindItem(ObjectIdGenerator.IdKey id, Object pojo) {
        if (items == null) {
            items = new HashMap<>();
        } else if (items.containsKey(id)) {
            log.warn("Already had POJO for id (" + pojo.getClass().getName() + ") [" + id.key + "]");
        }
        items.put(id, pojo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveId(final ObjectIdGenerator.IdKey id) {
        Object entity = (items != null && items.containsKey(id))
            ? items.get(id)
            : repository.findById((Long) id.key).orElse(null);

        if (entity == null) {
            throw new BusinessException(ErrorConstants.ERR_NOTFOUND, "Can not resolve XmEntity by ID: " + id.key);
        }

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectIdResolver newForDeserialization(final Object context) {
        return this;
    }

    @Override
    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }

}
