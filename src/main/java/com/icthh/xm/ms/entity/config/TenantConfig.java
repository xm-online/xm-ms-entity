package com.icthh.xm.ms.entity.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TenantConfig {
    private List<MailSettings> mailSettings;

    @Data
    public static class MailSettings {
        private String templateName;
        private Map<String, String> subject;
        private Map<String, String> from;
    }
}
