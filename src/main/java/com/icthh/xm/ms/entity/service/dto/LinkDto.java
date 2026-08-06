package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Schema(description = "Bidirectional link between two XmEntites.")
@Getter
@Setter
public class LinkDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "String typeKey with tree-like structure.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeKey;

    @Schema(description = "Link name")
    private String name;

    @Schema(description = "Link description")
    private String description;

    @Schema(description = "Start date")
    private Instant startDate;

    @Schema(description = "End date")
    private Instant endDate;

    @NotNull
    private XmEntityDto target;

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto source;

    @Schema(description = "Order")
    private Integer order;
}
