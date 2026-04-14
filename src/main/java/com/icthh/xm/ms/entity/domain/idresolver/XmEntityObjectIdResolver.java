package com.icthh.xm.ms.entity.domain.idresolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
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
public class XmEntityObjectIdResolver extends AbstractXmEntityObjectIdResolver {

    @Autowired
    public XmEntityObjectIdResolver(XmEntityRepository repository) {
        super(repository);
    }

    public XmEntityObjectIdResolver() {
        super();
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
        return new XmEntityObjectIdResolver(repository);
    }

}
