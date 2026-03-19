package com.icthh.xm.ms.entity.service.dto;

import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
public class VoteDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "Vote author user key", required = true)
    private String userKey;

    @NotNull
    @ApiModelProperty(value = "Vote value", required = true)
    private Double value;

    @ApiModelProperty(value = "Vote message")
    private String message;

    @ApiModelProperty(value = "Entry date")
    private Instant entryDate;

    private RatingDto rating;

    @NotNull
    private XmEntityDto xmEntity;
}
