package com.icthh.xm.ms.entity.service.impl;

import static com.icthh.xm.ms.entity.util.CustomCollectionUtils.nullSafe;

import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.service.impl.AbstractFunctionService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.function.FunctionSpecDto;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.security.access.FeatureContext;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonValidationService;
import com.icthh.xm.ms.entity.service.spec.FunctionMetaInfo;
import com.icthh.xm.ms.entity.util.CustomCollectionUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class XmEntityFunctionServiceImpl extends AbstractFunctionService<FunctionSpec> {

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityTenantConfigService xmEntityTenantConfigService;
    private final JsonValidationService jsonValidationService;

    public XmEntityFunctionServiceImpl(XmEntityDynamicPermissionCheckService dynamicPermissionCheckService,
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
    public Collection<FunctionSpecWithFileName<FunctionSpec>> getAllFunctionSpecs() {
        List<FunctionSpecWithFileName<FunctionSpec>> functionSpecs = new ArrayList<>();
        List<FunctionMetaInfo> allFunctionMetaInfo = xmEntitySpecService.findAllFunctionMetaInfo();
        for (FunctionMetaInfo metaInfo : nullSafe(allFunctionMetaInfo)) {
            FunctionSpec functionByKey = xmEntitySpecService.findFunctionByKey(metaInfo.functionKey());
            if (functionByKey != null) {
                FunctionSpecDto functionSpecDto = new FunctionSpecDto();
                functionSpecDto.setEntityTypeKey(metaInfo.entityTypeKey());
                functionSpecDto.setItem(functionByKey);
                functionSpecs.add(functionSpecDto);
            }
        }
        return functionSpecs;
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

    @Override
    public Collection<String> getAllFileNames() {
        return xmEntitySpecService.getAllFileNames();
    }
}
