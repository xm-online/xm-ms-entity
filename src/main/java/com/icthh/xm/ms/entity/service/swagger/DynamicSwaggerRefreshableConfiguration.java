package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.Data;
import org.springframework.util.AntPathMatcher;

public class DynamicSwaggerRefreshableConfiguration implements RefreshableConfiguration {

    private static final String SWAGGER_VERSION = "3.1.0";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void onRefresh(String updatedKey, String config) {

    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return false;
    }

    @Data
    public static class DynamicSwaggerConfiguration {

        private SwaggerInfo info;
        private ServerObject servers;


        public static class SwaggerInfo {
            private String version;
            private String title;
            private String description;
            private String termsOfService;
            private ContactObject contact;
            private LicenseObject license;

            @Data
            public static class ContactObject {
                private String name;
                private String url;
                private String email;
            }

            @Data
            public static class LicenseObject {
                private String name;
                private String url;
                private String identifier;
            }
        }

        @Data
        public static class ServerObject {
            String url;
            String description;
        }

    }
}
