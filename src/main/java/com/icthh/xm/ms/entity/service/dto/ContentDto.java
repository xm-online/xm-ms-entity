package com.icthh.xm.ms.entity.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Schema(description = "Represents Attachment content. Content can be extracted separately from the attachment.")
@Getter
@Setter
public class ContentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @Schema(description = "Content value as byte array", requiredMode = Schema.RequiredMode.REQUIRED)
    private byte[] value;
}
