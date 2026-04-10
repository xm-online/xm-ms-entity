package com.icthh.xm.ms.entity.service.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@ApiModel(description = "Represents Attachment content. Content can be extracted separately from the attachment.")
@Getter
@Setter
public class ContentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull
    @ApiModelProperty(value = "Content value as byte array", required = true)
    private byte[] value;
}
