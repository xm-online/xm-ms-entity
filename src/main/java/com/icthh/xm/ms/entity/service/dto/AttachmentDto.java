package com.icthh.xm.ms.entity.service.dto;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.icthh.xm.ms.entity.domain.idresolver.XmEntityObjectIdResolver;
import com.icthh.xm.ms.entity.validator.TypeKey;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@ApiModel(description = "Represents any XM entity file attachment. It could be image, zip archive, pdf document or other file formats (List of available file formats should be configured). Files should be verified on: - size - zip bombs - viruses")
@Getter
@Setter
@TypeKey
public class AttachmentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "String typeKey with tree-like structure.", required = true)
    private String typeKey;

    @NotNull
    @ApiModelProperty(value = "Attachment name", required = true)
    private String name;

    @ApiModelProperty(value = "Content retrieving URL")
    private String contentUrl;

    @ApiModelProperty(value = "Content description")
    private String description;

    @ApiModelProperty(value = "Start date")
    private Instant startDate;

    @ApiModelProperty(value = "End date")
    private Instant endDate;

    @ApiModelProperty(value = "Content type")
    private String valueContentType;

    @ApiModelProperty(value = "Content size in bytes")
    private Long valueContentSize;

    @ApiModelProperty(value = "Content checksum")
    private String contentChecksum;

    private ContentDto content;

    @NotNull
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", resolver = XmEntityObjectIdResolver.class)
    @JsonIdentityReference(alwaysAsId = true)
    private XmEntityDto xmEntity;
}
