package com.icthh.xm.ms.entity.domain.idresolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * XmEntity object ID resolver.
 * Resolves XmEntity object from database bi ID provided in JSON.
 * see https://stackoverflow.com/questions/41989906/jackson-referencing-an-object-as-a-property
 */
@Component
@Scope("prototype")
@Slf4j
public class XmEntityObjectIdResolver extends SimpleObjectIdResolver {

    private XmEntityRepository repository;

    @Autowired
    public XmEntityObjectIdResolver(XmEntityRepository repository) {
        this.repository = repository;
    }

    public XmEntityObjectIdResolver() {
        log.debug("XmEntity object id resolver inited");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveId(final ObjectIdGenerator.IdKey id) {
        Object entity = repository.findOne((Long) id.key);

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
        return new XmEntityObjectIdResolver(repository);
    }

}
