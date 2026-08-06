package com.icthh.xm.ms.entity.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Schema(description = "Represents User's profile on Entity microservice. Contains relation to XmEntity by foreign key. Profile will be creatid in LEP code as a reaction to event for new user registration.")
@Getter
@Setter
public class ProfileDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "User identifier from UAA.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userKey;

    @NotNull
    private XmEntityDto xmentity;
}
