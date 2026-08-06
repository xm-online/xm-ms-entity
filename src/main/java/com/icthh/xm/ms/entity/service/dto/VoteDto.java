package com.icthh.xm.ms.entity.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "Vote author user key", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userKey;

    @NotNull
    @Schema(description = "Vote value", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double value;

    @Schema(description = "Vote message")
    private String message;

    @Schema(description = "Entry date")
    private Instant entryDate;

    private RatingDto rating;

    @NotNull
    private XmEntityDto xmEntity;
}
