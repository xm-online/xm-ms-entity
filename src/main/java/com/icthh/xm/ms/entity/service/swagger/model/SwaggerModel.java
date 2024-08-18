package com.icthh.xm.ms.entity.service.swagger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SwaggerModel {

    public static final String SWAGGER_VERSION = "3.0.3";

    private final String openapi = SWAGGER_VERSION;
    private SwaggerInfo info = new SwaggerInfo();
    private List<ServerObject> servers;
    private List<TagObject> tags;
    private Map<String, Map<String, ApiMethod>> paths = new LinkedHashMap<>();
    private SwaggerComponents components = new SwaggerComponents();

    @Data
    public static class ApiMethod {
        private String summary;
        private String description;
        private String operationId;
        private List<String> tags;
        private List<SwaggerParameter> parameters;
        private Object requestBody;
        private Map<String, SwaggerResponse> responses;
    }

    @Data
    @AllArgsConstructor
    public static class RequestBody {
        private Boolean required;
        private RequestBodyContent content;
    }

    @Data
    @AllArgsConstructor
    public static class RequestBodyContent {
        @JsonProperty("application/json")
        private SwaggerContent applicationJson;
    }

    @Data
    public static class SwaggerComponents {
        private Map<String, Object> responses = new LinkedHashMap<>();
        private Map<String, Object> schemas = new LinkedHashMap<>();
        private Object securitySchemes;
    }

    @Data
    public static class SwaggerResponse {
        private String description;
        private Map<String, SwaggerContent> content;
        private String $ref;

        public SwaggerResponse() {
        }

        public SwaggerResponse(Map<String, SwaggerContent> content) {
            this.content = content;
        }

        public SwaggerResponse(Map<String, SwaggerContent> content, String description) {
            this.description = description;
            this.content = content;
        }

        public SwaggerResponse(String $ref) {
            this.$ref = $ref;
        }

        public String get$ref() {
            return $ref;
        }
    }

    @Data
    public static class SwaggerContent {
        private Map<String, Object> schema;

        public SwaggerContent() {
        }

        public SwaggerContent(Map<String, Object> schema) {
            this.schema = schema;
        }
    }

}
