package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@ApiModel(description = "Bidirectional link between two XmEntites.")
@Getter
@Setter
public class LinkDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    private String typeKey;

    @ApiModelProperty(value = "Link name")
    private String name;

    @ApiModelProperty(value = "Link description")
    private String description;

    @ApiModelProperty(value = "Start date")
    private Instant startDate;

    @ApiModelProperty(value = "End date")
    private Instant endDate;

    @NotNull
    private XmEntityDto target;

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto source;

    @ApiModelProperty(value = "Order")
    private Integer order;
}
