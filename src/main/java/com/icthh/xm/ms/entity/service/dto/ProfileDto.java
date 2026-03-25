package com.icthh.xm.ms.entity.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@ApiModel(description = "Represents User's profile on Entity microservice. Contains relation to XmEntity by foreign key. Profile will be creatid in LEP code as a reaction to event for new user registration.")
@Getter
@Setter
public class ProfileDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "User identifier from UAA.", required = true)
    private String userKey;

    @NotNull
    private XmEntityDto xmentity;
}
