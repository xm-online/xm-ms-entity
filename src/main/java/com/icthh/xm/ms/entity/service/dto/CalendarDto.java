package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@ApiModel(description = "Represents calendar instance related to XmEntity.")
@Getter
@Setter
@TypeKey
public class CalendarDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    private String typeKey;

    @NotNull
    @ApiModelProperty(value = "Calendar name.", required = true)
    private String name;

    @ApiModelProperty(value = "Calendar description")
    private String description;

    @ApiModelProperty(value = "Start date")
    private Instant startDate;

    @ApiModelProperty(value = "End date")
    private Instant endDate;

    private Set<EventDto> events = new HashSet<>();

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;

    @ApiModelProperty(value = "Timezone id")
    private String timeZoneId;
}
