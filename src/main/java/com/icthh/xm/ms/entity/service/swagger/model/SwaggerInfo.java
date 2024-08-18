package com.icthh.xm.ms.entity.service.swagger.model;

import lombok.Data;

@Data
public class SwaggerInfo {
    private String version;
    private String title;
    private String description;
    private String termsOfService;
    private ContactObject contact;
    private LicenseObject license;

    public SwaggerInfo() {
        this.version = "0.0.1";
        this.title = "XM Entity functions api";
    }

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
