package com.icthh.xm.ms.entity.service.swagger.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class SwaggerModel {

    public static final String SWAGGER_VERSION = "3.0.3";

    private final String openapi = SWAGGER_VERSION;
    private SwaggerInfo info = new SwaggerInfo();
    private List<ServerObject> servers = new ArrayList<>();
    private List<TagObject> tags = new ArrayList<>();
    private Map<String, Map<String, ApiMethod>> paths = new LinkedHashMap<>();
    private SwaggerComponents components = new SwaggerComponents();
    @JsonInclude()
    private List<Map<String, List<Object>>> security = new ArrayList<>();

    {
        security.add(Map.of("oAuth2Password", new ArrayList<>()));
        security.add(Map.of("oAuth2ClientCredentials", new ArrayList<>()));
    }

    @Data
    public static class ApiMethod {
        private String summary;
        private String description;
        private String operationId;
        private List<String> tags = new ArrayList<>();
        private List<SwaggerParameter> parameters = new ArrayList<>();
        private Object requestBody;
        private Map<String, SwaggerResponse> responses = new LinkedHashMap<>();
    }

    @Data
    public static class RequestBody {
        private Boolean required;
        private Object content;

        public RequestBody() {}

        public RequestBody(Boolean required, BodyContent content) {
            this.content = content;
            this.required = required;
        }

        public RequestBody(Boolean required, Map<String, SwaggerContent> content) {
            this.content = content;
            this.required = required;
        }

    }

    @Data
    @AllArgsConstructor
    public static class ResponseBody {
        private BodyContent content;

        public ResponseBody() {}
    }

    @Data
    @AllArgsConstructor
    public static class BodyContent {
        @JsonProperty("application/json")
        private SwaggerContent applicationJson;

        public BodyContent() {}
    }

    @Data
    public static class SwaggerComponents {
        private Map<String, Object> responses = new LinkedHashMap<>();
        private Map<String, Object> schemas = new LinkedHashMap<>();
        private Map<String, SecuritySchemes> securitySchemes = new LinkedHashMap<>();

        {
            securitySchemes.put("oAuth2Password", new SecuritySchemes("password"));
            securitySchemes.put("oAuth2ClientCredentials", new SecuritySchemes("clientCredentials"));
        }

    }

    @Data
    public static class SecuritySchemes {
        private String type = "oauth2";
        private Map<String, SecurityFlow> flows = new HashMap<>();

        public SecuritySchemes() {}

        public SecuritySchemes(String flow) {
            flows.put(flow, new SecurityFlow());
        }
    }

    @Data
    public static class SecurityFlow {
        private String tokenUrl = "/uaa/oauth/token";
        private Map<String, String> scopes = new HashMap<>() {{
            put("openapi", "Default client scope");
        }};
    }

    @Data
    public static class SwaggerResponse {
        private String description;
        private Object content;
        private String $ref;

        public SwaggerResponse() {
        }

        public SwaggerResponse(BodyContent content) {
            this.content = content;
        }

        public SwaggerResponse(Map<String, SwaggerContent> content, String description) {
            this.content = content;
            this.description = description;
        }

        public SwaggerResponse(BodyContent content, String description) {
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
