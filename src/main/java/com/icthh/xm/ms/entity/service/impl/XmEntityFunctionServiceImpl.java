package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.permission.service.DynamicPermissionCheckService;
import com.icthh.xm.commons.service.impl.AbstractFunctionService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.security.access.FeatureContext;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.util.CustomCollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class XmEntityFunctionServiceImpl extends AbstractFunctionService<FunctionSpec> {

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityTenantConfigService xmEntityTenantConfigService;
    private final JsonValidationService jsonValidationService;

    public XmEntityFunctionServiceImpl(DynamicPermissionCheckService dynamicPermissionCheckService,
                                       XmEntitySpecService xmEntitySpecService,
                                       XmEntityTenantConfigService xmEntityTenantConfigService,
                                       JsonValidationService jsonValidationService) {
        super(dynamicPermissionCheckService);
        this.xmEntitySpecService = xmEntitySpecService;
        this.xmEntityTenantConfigService = xmEntityTenantConfigService;
        this.jsonValidationService = jsonValidationService;
    }

    @Override
    public FunctionSpec findFunctionSpec(String functionKey, String httpMethod) {
        return xmEntitySpecService.findFunction(functionKey, httpMethod).orElseThrow(
            () -> new IllegalArgumentException("Function not found, function key: " + functionKey));
    }

    @Override
    public void checkPermissions(String basePermission, String functionKey) {
        super.checkPermissions(FeatureContext.FUNCTION, basePermission, functionKey);
    }

    @Override
    public Map<String, Object> getValidFunctionInput(FunctionSpec functionSpec, Map<String, Object> functionInput) {
        Map<String, Object> vInput = CustomCollectionUtils.emptyIfNull(functionInput);
        if (xmEntityTenantConfigService.getXmEntityTenantConfig().getEntityFunctions().getValidateFunctionInput()) {
            // exclude one when enabled for all
            if (!Boolean.FALSE.equals(functionSpec.getValidateFunctionInput())) {
                jsonValidationService.assertJson(vInput, functionSpec.getInputSpec());
            }
        } else {
            // include one when disabled for all
            if (Boolean.TRUE.equals(functionSpec.getValidateFunctionInput())) {
                jsonValidationService.assertJson(vInput, functionSpec.getInputSpec());
            }
        }
        return vInput;
    }
}
