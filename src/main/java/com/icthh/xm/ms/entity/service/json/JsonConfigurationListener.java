package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.listener.AbstractJsonConfigurationListener;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JsonConfigurationListener extends AbstractJsonConfigurationListener {

    public static final String XM_ENTITY_SPEC_KEY = "xmentityspec";

    private final XmEntitySpecContextService xmEntitySpecContextService;

    public JsonConfigurationListener(@Value("${spring.application.name}") String appName,
                                     XmEntitySpecContextService xmEntitySpecContextService,
                                     JsonListenerService jsonListenerService) {
        super("/config/tenants/{tenantName}/" + appName + "/xmentityspec/**/*.json", jsonListenerService);
        this.xmEntitySpecContextService = xmEntitySpecContextService;
    }

    @Override
    public String getSpecificationKey() {
        return XM_ENTITY_SPEC_KEY;
    }

    @Override
    public void updateByTenantState(String tenant) {
        xmEntitySpecContextService.updateByTenantState(tenant);
    }
}
