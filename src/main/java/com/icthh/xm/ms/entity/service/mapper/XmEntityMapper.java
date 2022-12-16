package com.icthh.xm.ms.entity.service.mapper;

import com.icthh.xm.commons.domainevent.domain.DomainEvent;
import com.icthh.xm.ms.entity.domain.XmEntity;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface XmEntityMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "txId", ignore = true)
    @Mapping(target = "eventDate", ignore = true)
    @Mapping(target = "aggregateId", ignore = true)
    @Mapping(target = "aggregateType", ignore = true)
    @Mapping(target = "operation", ignore = true)
    @Mapping(target = "msName", ignore = true)
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "userKey", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "validFor", ignore = true)
    @Mapping(target = "meta", ignore = true)
    @Mapping(target = "payload", ignore = true)
    DomainEvent toEvent(XmEntity entity);

    @IterableMapping(elementTargetType = DomainEvent.class)
    List<DomainEvent> toEvent(List<XmEntity> entity);
}
