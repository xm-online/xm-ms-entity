package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.WithTypeKey;
import com.icthh.xm.ms.entity.domain.idresolver.CalendarDtoObjectIdResolver;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import com.icthh.xm.ms.entity.validator.EventDataTypeKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@EventDataTypeKey
public class EventDto implements Serializable, WithTypeKey {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "String typeKey with tree-like structure.")
    private String typeKey;

    @Schema(description = "Configuration for event repetition")
    private String repeatRuleKey;

    @NotNull
    @Schema(description = "Event title", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Event description")
    private String description;

    @Schema(description = "Start date")
    private Instant startDate;

    @Schema(description = "End date")
    private Instant endDate;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = CalendarDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private CalendarDto calendar;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto assigned;

    @Schema(description = "Reference to event's extra data")
    private XmEntityDto eventDataRef;

    @Schema(description = "Event color")
    private String color;
}
