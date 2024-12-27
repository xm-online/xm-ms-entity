package com.icthh.xm.ms.entity.security.access;

import com.icthh.xm.commons.permission.domain.enums.IFeatureContext;
import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import lombok.AllArgsConstructor;

import java.util.function.Function;

/**
 * Feature switcher implementation
 */
@AllArgsConstructor
public enum FeatureContext implements IFeatureContext {

    FUNCTION(XmEntityDynamicPermissionCheckService::isDynamicFunctionPermissionEnabled),
    CHANGE_STATE(XmEntityDynamicPermissionCheckService::isDynamicChangeStatePermissionEnabled),
    LINK_DELETE(XmEntityDynamicPermissionCheckService::isDynamicLinkDeletePermissionEnabled);

    private final Function<XmEntityDynamicPermissionCheckService, Boolean> featureContextResolver;

    /**
     * Checks if feature FUNCTION|CHANGE_STATE enabled in tenant config
     * @param service - DynamicPermissionCheckService instance
     * @return true if enabled
     */
    @Override
    public boolean isEnabled(DynamicPermissionCheckService service) {
        if (service instanceof XmEntityDynamicPermissionCheckService) {
            return this.featureContextResolver.apply((XmEntityDynamicPermissionCheckService) service);
        }
        throw new IllegalArgumentException("Invalid check service type: " + service.getClass().getName());
    }
}
