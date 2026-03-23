package com.icthh.xm.ms.entity.domain.idresolver;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.annotation.SimpleObjectIdResolver;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.ms.entity.repository.CalendarRepository;
import com.icthh.xm.ms.entity.service.dto.CalendarDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * CalendarDto object ID resolver.
 * Resolves CalendarDto from database by ID provided in JSON.
 * Returns CalendarDto (shallow, ID only) instead of Calendar to match DTO field types.
 */
@Component
@Scope("prototype")
@Slf4j
public class CalendarDtoObjectIdResolver extends SimpleObjectIdResolver {

    CalendarRepository repository;

    @Autowired
    public CalendarDtoObjectIdResolver(CalendarRepository repository) {
        this.repository = repository;
    }

    public CalendarDtoObjectIdResolver() {
        log.debug("CalendarDto object id resolver inited");
    }

    @Override
    public Object resolveId(final ObjectIdGenerator.IdKey id) {
        if (!repository.existsById((Long) id.key)) {
            throw new BusinessException(ErrorConstants.ERR_NOTFOUND,
                "Can not resolve Calendar by ID: " + id.key);
        }
        CalendarDto dto = new CalendarDto();
        dto.setId((Long) id.key);
        return dto;
    }

    @Override
    public ObjectIdResolver newForDeserialization(final Object context) {
        return new CalendarDtoObjectIdResolver(repository);
    }
}
