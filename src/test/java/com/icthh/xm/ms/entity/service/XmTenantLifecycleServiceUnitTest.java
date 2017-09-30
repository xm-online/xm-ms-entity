package com.icthh.xm.ms.entity.service;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.EntityState;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.XmLepScriptConstants;
import com.icthh.xm.ms.entity.service.XmTenantLifecycleService.ServiceInfo;
import com.icthh.xm.ms.entity.web.client.tenant.TenantClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class XmTenantLifecycleServiceUnitTest {

    private static final String ENTITY_TYPE_KEY = "RESOURCE.XM-TENANT";
    private static final String SERVICE_NAME = "test";
    private static final String EXEC_ERROR = "Service call failed";
    private static final String SUCCESS_NAME = "success";

    private XmTenantLifecycleService xmTenantLifecycleService;
    private List<TenantClient> tenantClients = new ArrayList<>();
    private Map<String, Object> context = new HashMap<>();

    @Mock
    private ApplicationProperties applicationProperties;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        xmTenantLifecycleService = new XmTenantLifecycleService(tenantClients, applicationProperties);
    }

    @Test
    public void testNoContext() {
        XmEntity xmEntity = new XmEntity().typeKey(ENTITY_TYPE_KEY).stateKey(EntityState.NEW.name());

        xmTenantLifecycleService.changeState(xmEntity, EntityState.ACTIVE.name(), context);

        assertEquals(0, xmEntity.getData().size());
    }

    @Test
    public void testNoService() {
        XmEntity xmEntity = new XmEntity().typeKey(ENTITY_TYPE_KEY).stateKey(EntityState.NEW.name());
        context.put(XmLepScriptConstants.BINDING_KEY_SERVICES, Collections.singletonList(SERVICE_NAME));

        xmTenantLifecycleService.changeState(xmEntity, EntityState.ACTIVE.name(), context);

        assertEquals(1, xmEntity.getData().size());
        assertEquals(false, ((ServiceInfo) ((Map) xmEntity.getData().get(SERVICE_NAME)).get("create")).isSuccess());
        assertEquals("Service client not found: test",
            ((ServiceInfo) ((Map) xmEntity.getData().get(SERVICE_NAME)).get("create")).getErrorMessage());
    }

    @Test
    public void testServiceCallFail() {
        XmEntity xmEntity = new XmEntity().typeKey(ENTITY_TYPE_KEY).stateKey(EntityState.NEW.name());
        context.put(XmLepScriptConstants.BINDING_KEY_SERVICES, Collections.singletonList(SERVICE_NAME));
        TenantClient client = new FailClient();
        tenantClients.add(client);

        xmTenantLifecycleService.changeState(xmEntity, EntityState.ACTIVE.name(), context);

        assertEquals(1, xmEntity.getData().size());
        assertEquals(false, ((ServiceInfo) ((Map) xmEntity.getData().get(SERVICE_NAME)).get("create")).isSuccess());
        assertEquals(EXEC_ERROR, ((ServiceInfo) ((Map) xmEntity.getData().get(SERVICE_NAME)).get("create")).getErrorMessage());
    }

    @Test
    public void testServiceCallPass() {
        XmEntity xmEntity = new XmEntity().typeKey(ENTITY_TYPE_KEY).stateKey(EntityState.NEW.name());
        Map<String, Object> serviceInfo = new HashMap<>();
        Map<String, Object> action = new HashMap<>();
        serviceInfo.put("create", action);
        action.put(SUCCESS_NAME, true);
        xmEntity.getData().put(SERVICE_NAME, serviceInfo);
        context.put(XmLepScriptConstants.BINDING_KEY_SERVICES, Collections.singletonList(SERVICE_NAME));
        TenantClient client = new FailClient();
        tenantClients.add(client);

        xmTenantLifecycleService.changeState(xmEntity, EntityState.ACTIVE.name(), context);

        assertEquals(1, xmEntity.getData().size());
        assertEquals(true, ((Map) ((Map) xmEntity.getData().get(SERVICE_NAME)).get("create")).get(SUCCESS_NAME));
    }

    @Test
    public void testServiceCallSuccess() {
        XmEntity xmEntity = new XmEntity().typeKey(ENTITY_TYPE_KEY).stateKey(EntityState.NEW.name());
        context.put(XmLepScriptConstants.BINDING_KEY_SERVICES, Collections.singletonList(SERVICE_NAME));
        TenantClient client = new SuccessClient();
        tenantClients.add(client);

        xmTenantLifecycleService.changeState(xmEntity, EntityState.ACTIVE.name(), context);

        assertEquals(1, xmEntity.getData().size());
        assertEquals(true, ((ServiceInfo) ((Map) xmEntity.getData().get(SERVICE_NAME)).get("create")).isSuccess());
    }

    private static class FailClient implements TenantClient {

        @Override
        public String getName() {
            return SERVICE_NAME;
        }

        @Override
        public void addTenant(Tenant tenant) {
            throw new RuntimeException(EXEC_ERROR);
        }

        public void deleteTenant(String var1) {

        }

        public List<Tenant> getAllTenantInfo() {
            return null;
        }

        public Tenant getTenant(String var1) {
            return null;
        }

        public void manageTenant(String var1, String var2){

        }
    }

    private static class SuccessClient implements TenantClient {

        @Override
        public String getName() {
            return SERVICE_NAME;
        }

        @Override
        public void addTenant(Tenant tenant) {
        }

        public void deleteTenant(String var1) {

        }

        public List<Tenant> getAllTenantInfo() {
            return null;
        }

        public Tenant getTenant(String var1) {
            return null;
        }

        public void manageTenant(String var1, String var2) {

        }
    }

}
