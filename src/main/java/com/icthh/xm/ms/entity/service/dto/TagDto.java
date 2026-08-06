package com.icthh.xm.ms.entity.service.dto;

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

@Schema(description = "Represents tags associated with the XmEntity.")
@Getter
@Setter
@TypeKey
public class TagDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "String typeKey with tree-like structure.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeKey;

    @Schema(description = "Searhable Tag's name")
    private String name;

    @Schema(description = "Start date")
    private Instant startDate;

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;
}
