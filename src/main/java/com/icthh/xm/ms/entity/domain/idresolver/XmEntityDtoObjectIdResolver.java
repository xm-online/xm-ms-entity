package com.icthh.xm.ms.entity.domain.idresolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * XmEntityDto object ID resolver.
 * Resolves XmEntityDto from database by ID provided in JSON.
 * Returns XmEntityDto (shallow, ID only) instead of XmEntity to match DTO field types.
 */
@Slf4j
@Component
@Scope("prototype")
public class XmEntityDtoObjectIdResolver extends SimpleObjectIdResolver {
    protected Map<ObjectIdGenerator.IdKey, Object> items;

    private XmEntityRepository repository;

    @Autowired
    public XmEntityDtoObjectIdResolver(XmEntityRepository repository) {
        this.repository = repository;
    }

    public XmEntityDtoObjectIdResolver() {
        log.debug("XmEntityDto object id resolver inited");
    }

    @Override
    public Object resolveId(final ObjectIdGenerator.IdKey id) {
        Object resolved = (items != null && items.containsKey(id))
            ? items.get(id)
            : null;

        if (resolved != null) {
            return resolved;
        }

        var entity = repository.findById((Long) id.key).orElse(null);
        if (entity == null) {
            throw new BusinessException(ErrorConstants.ERR_NOTFOUND, "Can not resolve XmEntity by ID: " + id.key);
        }

        XmEntityDto dto = new XmEntityDto();
        dto.setId(entity.getId());
        dto.setTypeKey(entity.getTypeKey());
        return dto;
    }

    @Override
    public ObjectIdResolver newForDeserialization(final Object context) {
        return new XmEntityDtoObjectIdResolver(repository);
    }

    @Override
    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }
}