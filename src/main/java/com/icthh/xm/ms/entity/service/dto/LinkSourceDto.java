package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.serializer.SimpleXmEntitySerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.Optional;

/**
 * DTO for Link entity to be user for source links response.
 */
@Getter
@Setter
@ToString
public class LinkSourceDto {

    public LinkSourceDto(Link link) {

        if (link != null) {
            this.id = link.getId();
            this.typeKey = link.getTypeKey();
            this.name = link.getName();
            this.description = link.getDescription();
            this.startDate = link.getStartDate();
            this.endDate = link.getEndDate();
            this.target = Optional.ofNullable(link.getTarget()).map(XmEntity::getId).orElse(null);
            this.source = link.getSource();

        }
    }

    private Long id;

    private String typeKey;

    private String name;

    private String description;

    private Instant startDate;

    private Instant endDate;

    private Long target;

    @JsonSerialize(using = SimpleXmEntitySerializer.class)
    private XmEntity source;

}
