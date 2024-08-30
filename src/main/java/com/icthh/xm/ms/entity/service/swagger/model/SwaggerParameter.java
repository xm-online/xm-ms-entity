package com.icthh.xm.ms.entity.service.swagger.model;

import lombok.Data;

import java.util.Map;

@Data
public class SwaggerParameter {
    private String in = "path";
    private String name;
    private Boolean required;
    private Map<String, Object> schema;

    public SwaggerParameter() {
    }

    public SwaggerParameter(String idOrKey, boolean required, Map<String, Object> schema) {
        this.name = idOrKey;
        this.required = required;
        this.schema = schema;
    }

    public SwaggerParameter(String in, String name, Boolean required, Map<String, Object> schema) {
        this.in = in;
        this.name = name;
        this.required = required;
        this.schema = schema;
    }
}
