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

@Schema(description = "Represents any XM entity file attachment. It could be image, zip archive, pdf document or other file formats (List of available file formats should be configured). Files should be verified on: - size - zip bombs - viruses")
@Getter
@Setter
@TypeKey
public class AttachmentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "String typeKey with tree-like structure.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String typeKey;

    @NotNull
    @Schema(description = "Attachment name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Content retrieving URL")
    private String contentUrl;

    @Schema(description = "Content description")
    private String description;

    @Schema(description = "Start date")
    private Instant startDate;

    @Schema(description = "End date")
    private Instant endDate;

    @Schema(description = "Content type")
    private String valueContentType;

    @Schema(description = "Content size in bytes")
    private Long valueContentSize;

    @Schema(description = "Content checksum")
    private String contentChecksum;

    private ContentDto content;

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityDtoObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;
}
