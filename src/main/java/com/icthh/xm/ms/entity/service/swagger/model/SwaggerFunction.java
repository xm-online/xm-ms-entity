package com.icthh.xm.ms.entity.service.swagger.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class SwaggerFunction {
    private String operationId;
    private String path;
    private String name;
    private String inputJsonSchema;
    private String outputJsonSchema;
    private List<String> tags = new ArrayList<>();
    private List<String> httpMethods = new ArrayList<>();

    /**
     type: string, format: binary =>
     responseBody:
     content:
     custom/content-type:
     schema:
     type: string
     format: binary
     */
    private String customBinaryDataType;

}
