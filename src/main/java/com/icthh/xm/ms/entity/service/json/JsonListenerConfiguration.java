package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JsonListenerConfiguration implements RefreshableConfiguration {

    private final String mappingPath;
    private final AntPathMatcher matcher;
    private final XmEntitySpecContextService xmEntitySpecContextService;
    private final JsonListenerService jsonListenerService;

    private static final String TENANT_NAME = "tenantName";

    public JsonListenerConfiguration(@Value("${spring.application.name}") String appName,
                                     XmEntitySpecContextService xmEntitySpecContextService,
                                     JsonListenerService jsonListenerService) {
        this.xmEntitySpecContextService = xmEntitySpecContextService;
        this.jsonListenerService = jsonListenerService;
        this.matcher = new AntPathMatcher();
        this.mappingPath = "/config/tenants/{tenantName}/" + appName + "/xmentityspec/**/*.json";
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String tenantName = extractTenantName(updatedKey);
        String relativePath = updatedKey.substring(updatedKey.indexOf("xmentityspec"));

        jsonListenerService.processTenantSpecification(tenantName, relativePath, config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return matcher.match(mappingPath, updatedKey);
    }

    @Override
    public void refreshFinished(Collection<String> paths) {
        List<String> tenants = paths.stream().map(this::extractTenantName).collect(Collectors.toList());
        tenants.forEach(xmEntitySpecContextService::updateByTenantState);
    }

    private String extractTenantName(String updatedKey) {
        return matcher.extractUriTemplateVariables(mappingPath, updatedKey).get(TENANT_NAME);
    }
}
