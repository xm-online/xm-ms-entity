package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.web.rest.FunctionSpecResource;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.function.FunctionSpecDto;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class XmEntityFunctionManagementServiceIntTest extends AbstractSpringBootTest {

    @MockBean
    private CommonConfigRepository commonConfigRepository;
    @MockBean
    private CommonConfigService commonConfigService;

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManagementService lepManagementService;
    @Autowired
    XmEntitySpecService xmEntitySpecService;
    @Autowired XmEntityFunctionManagementService managementService;
    @Autowired
    FunctionSpecResource functionSpecResource;

    @Before
    public void setUp() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManagementService.beginThreadContext();
    }

    @After
    public void tearDown() {
        lepManagementService.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void addFunctionWhereNoFunctionsBlockExists() {
        String path = "/config/tenants/RESINTTEST/entity/xmentityspec/test-spec.yml";
        xmEntitySpecService.onRefresh(path, loadFile("config/specs/test-spec.yml"));
        xmEntitySpecService.refreshFinished(List.of(path));

        System.out.println(functionSpecResource);
        Mockito.reset(commonConfigRepository);

        FunctionSpecDto functionSpecDto = new FunctionSpecDto();
        functionSpecDto.setEntityTypeKey("TEST_TYPE");
        functionSpecDto.setItem(mockSpec());
        managementService.addFunction(functionSpecDto);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(path, c.getPath());
            assertEquals(loadFile("config/specs/test-spec-no-f-expected.yml"), c.getContent());
            return true;
        }), isNull());
    }

    @Test
    public void addFunctionWhereEmptyFunctionsBlockExists() {
        String path = "/config/tenants/RESINTTEST/entity/xmentityspec/test-spec.yml";
        xmEntitySpecService.onRefresh(path, loadFile("config/specs/test-spec.yml"));
        xmEntitySpecService.refreshFinished(List.of(path));

        Mockito.reset(commonConfigRepository);

        FunctionSpecDto functionSpecDto = new FunctionSpecDto();
        functionSpecDto.setEntityTypeKey("TEST_TYPE_WITH_FUNCTIONS");
        functionSpecDto.setItem(mockSpec());
        managementService.addFunction(functionSpecDto);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(path, c.getPath());
            assertEquals(loadFile("config/specs/test-spec-empty-f-expected.yml"), c.getContent());
            return true;
        }), isNull());
    }

    @Test
    public void addFunctionWhereFunctionsNotEmpty() {
        String path = "/config/tenants/RESINTTEST/entity/xmentityspec/test-spec.yml";
        xmEntitySpecService.onRefresh(path, loadFile("config/specs/test-spec.yml"));
        xmEntitySpecService.refreshFinished(List.of(path));

        Mockito.reset(commonConfigRepository);

        FunctionSpecDto functionSpecDto = new FunctionSpecDto();
        functionSpecDto.setEntityTypeKey("TEST_TYPE_WITH_EXISTS_FUNCTIONS");
        functionSpecDto.setItem(mockSpec());
        managementService.addFunction(functionSpecDto);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(path, c.getPath());
            assertEquals(loadFile("config/specs/test-spec-with-f-expected.yml"), c.getContent());
            return true;
        }), isNull());
    }

    private FunctionSpec mockSpec() {
        FunctionSpec functionSpec = new FunctionSpec();
        functionSpec.setKey("flow/http/flow-http-trigger-PUT-UPDATE_PHONE_NUMBER_RESOURCE");
        functionSpec.setPath("resource/{orgRef}/phoneNumber/{phoneNumber}");
        functionSpec.setInputSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        functionSpec.setTxType(FunctionTxTypes.NO_TX);
        functionSpec.setTags(List.of("resource"));
        functionSpec.setHttpMethods(List.of("PUT"));
        functionSpec.setName(Map.of("en", "Update phone number resource"));
        functionSpec.setDescription("Update phone number resource");
        functionSpec.setContextDataSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        return functionSpec;
    }

    private FunctionSpec mockFunc2() {
        FunctionSpec functionSpec = new FunctionSpec();
        functionSpec.setKey("FUNC_2");
        functionSpec.setPath("func2");
        functionSpec.setInputSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        functionSpec.setTxType(FunctionTxTypes.NO_TX);
        functionSpec.setTags(List.of("resource"));
        functionSpec.setHttpMethods(List.of("PUT", "GET", "POST"));
        functionSpec.setAnonymous(true);
        functionSpec.setName(Map.of("en", "func2"));
        functionSpec.setDescription("func2");
        functionSpec.setContextDataSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        return functionSpec;
    }

}
