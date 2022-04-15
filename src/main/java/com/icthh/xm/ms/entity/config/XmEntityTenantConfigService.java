package com.icthh.xm.ms.entity.config;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class XmEntityTenantConfigService extends TenantConfigService {

    private final Map<String, XmEntityTenantConfig> configs = new ConcurrentHashMap<>();
    private final TenantContextHolder tenantContextHolder;

    public XmEntityTenantConfigService(XmConfigProperties xmConfigProperties, TenantContextHolder tenantContextHolder) {
        super(xmConfigProperties, tenantContextHolder);
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    @SneakyThrows
    public void onRefresh(String updatedKey, String config) {
        try {
            super.onRefresh(updatedKey, config);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            config = skipNullFIelds(config, objectMapper);
            XmEntityTenantConfig value = objectMapper.readValue(config, XmEntityTenantConfig.class);
            configs.put(getTenantKey(updatedKey), value);
        } catch (Exception e) {
            log.error("Error read tenant configuration from path [{}]", updatedKey, e);
        }
    }

    private String skipNullFIelds(String config, ObjectMapper objectMapper) throws java.io.IOException {
        objectMapper.setSerializationInclusion(NON_NULL);
        return objectMapper.writeValueAsString(objectMapper.readValue(config, Map.class));
    }

    public XmEntityTenantConfig getXmEntityTenantConfig() {
        String tenantKey = tenantContextHolder.getTenantKey();
        configs.computeIfAbsent(tenantKey, (key) -> new XmEntityTenantConfig());
        return configs.get(tenantKey);
    }

    public XmEntityTenantConfig getXmEntityTenantConfig(String tenantKey) {
        return configs.computeIfAbsent(tenantKey, (key) -> new XmEntityTenantConfig());
    }

    /**
     * Configuration class for enable/disable tenant level feature in xm-entity.
     * Keep resources/specs/tenant-config-json.schema.json actual.
     */
    @Data
    public static class XmEntityTenantConfig {

        private Boolean disableDynamicPrivilegesGeneration = false;

        @JsonProperty("entity-functions")
        private EntityFunctions entityFunctions = new EntityFunctions();
        @Data
        public static class EntityFunctions {
            private Boolean dynamicPermissionCheckEnabled = false;
            private Boolean validateFunctionInput = false;
        }

        private DynamicTypeKeyPermission dynamicTypeKeyPermission = new DynamicTypeKeyPermission();
        @Data
        public static class DynamicTypeKeyPermission {
            private Boolean linkDeletion = false;
        }

        private EntityVersionControl entityVersionControl = new EntityVersionControl();
        @Data
        public static class EntityVersionControl {
            private Boolean enabled = false;
        }

        private List<MailSetting> mailSettings = new ArrayList<>();
        @Data
        public static class MailSetting {
            private String templateName;
            private Map<String, String> subject = new HashMap<>();
            private Map<String, String> from = new HashMap<>();
        }

        private LepSetting lep = new LepSetting();
        @Data
        public static class LepSetting {
            private Boolean enableInheritanceTypeKey = false;
        }

        private EntitySpec entitySpec = new EntitySpec();
        @Data
        public static class EntitySpec {
            private Boolean enableDataSpecInheritance = false;
            private Boolean enableDataFromInheritance = false;
        }
    }

}
