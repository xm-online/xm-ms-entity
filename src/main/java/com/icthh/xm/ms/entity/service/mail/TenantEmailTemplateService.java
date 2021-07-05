package com.icthh.xm.ms.entity.service.mail;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.logging.LoggingAspectConfig;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing email template.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantEmailTemplateService implements RefreshableConfiguration {

    private static final String FILE_NAME = "fileName";
    private static final String LANG_KEY = "langKey";
    private static final String TENANT_NAME = "tenantName";

    private ConcurrentHashMap<String, String> emailTemplates = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final ApplicationProperties applicationProperties;
    private final StringTemplateLoader templateLoader;

    /**
     * Search email template by email template key.
     *
     * @param emailTemplateKey search key
     * @return email template
     */
    @LoggingAspectConfig(resultDetails = false)
    public String getEmailTemplate(String emailTemplateKey) {
        if (!emailTemplates.containsKey(emailTemplateKey)) {
            throw new IllegalArgumentException("Email template was not found");
        }
        return emailTemplates.get(emailTemplateKey);
    }

    @Override
    public void onRefresh(String key, String config) {
        String pathPattern = applicationProperties.getEmailPathPattern();

        String tenantKeyValue = matcher.extractUriTemplateVariables(pathPattern, key).get(TENANT_NAME);
        String langKey = matcher.extractUriTemplateVariables(pathPattern, key).get(LANG_KEY);
        String templateName = matcher.extractUriTemplateVariables(pathPattern, key).get(FILE_NAME);

        String templateKey = EmailTemplateUtil.emailTemplateKey(TenantKey.valueOf(tenantKeyValue),
                                                                langKey,
                                                                templateName);
        if (StringUtils.isBlank(config)) {
            emailTemplates.remove(templateKey);
            templateLoader.removeTemplate(templateKey);
            log.info("Email template '{}' with locale {} for tenant '{}' was removed", templateName,
                            langKey, tenantKeyValue);
        } else {
            emailTemplates.put(templateKey, config);
            templateLoader.putTemplate(templateKey, config);
            log.info("Email template '{}' with locale {} for tenant '{}' was updated", templateName,
                            langKey, tenantKeyValue);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPathPattern = applicationProperties.getEmailPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        if (isListeningConfiguration(key)) {
            onRefresh(key, config);
        }
    }
}
