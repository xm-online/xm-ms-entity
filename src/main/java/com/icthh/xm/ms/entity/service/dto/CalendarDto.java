package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Schema(description = "Represents calendar instance related to XmEntity.")
@Getter
@Setter
@TypeKey
public class CalendarDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "String typeKey with tree-like structure.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeKey;

    @NotNull
    @Schema(description = "Calendar name.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Calendar description")
    private String description;

    @Schema(description = "Start date")
    private Instant startDate;

    @Schema(description = "End date")
    private Instant endDate;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Set<EventDto> events = new HashSet<>();

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;

    @Schema(description = "Timezone id")
    private String timeZoneId;
}
