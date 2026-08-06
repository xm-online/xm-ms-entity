package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityDtoObjectIdResolver;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class CommentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @Schema(description = "Comment author user key")
    private String userKey;

    @Schema(description = "Comment author client id")
    private String clientId;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Message content")
    private String message;

    @Schema(description = "Entry date")
    private Instant entryDate;

    private CommentDto comment;

    @JsonIgnore
    private Set<CommentDto> replies = new HashSet<>();

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;
}
