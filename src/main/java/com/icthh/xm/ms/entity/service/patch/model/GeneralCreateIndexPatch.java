package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class GeneralCreateIndexPatch extends CreateIndexPatch {
    @NotNull
    @NotBlank
    private String indexExpression;
}
