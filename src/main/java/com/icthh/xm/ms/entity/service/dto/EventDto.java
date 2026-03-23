package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.CalendarDtoObjectIdResolver;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import com.icthh.xm.ms.entity.validator.EventDataTypeKey;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@EventDataTypeKey
public class EventDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.")
    private String typeKey;

    @ApiModelProperty(value = "Configuration for event repetition")
    private String repeatRuleKey;

    @NotNull
    @ApiModelProperty(value = "Event title", required = true)
    private String title;

    @ApiModelProperty(value = "Event description")
    private String description;

    @ApiModelProperty(value = "Start date")
    private Instant startDate;

    @ApiModelProperty(value = "End date")
    private Instant endDate;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = CalendarDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private CalendarDto calendar;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto assigned;

    @ApiModelProperty(value = "Reference to event's extra data")
    private XmEntityDto eventDataRef;

    @ApiModelProperty(value = "Event color")
    private String color;
}
