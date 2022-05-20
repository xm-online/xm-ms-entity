package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Data
public class JsonPathCreateIndexPatch extends CreateIndexPatch {
    @NotNull
    @NotBlank
    private String jsonPath;

    @Override
    public String getIndexExpression() {
        return "jsonb_path_query_first(data, '" + jsonPath + "'::jsonpath)";
    }
}
