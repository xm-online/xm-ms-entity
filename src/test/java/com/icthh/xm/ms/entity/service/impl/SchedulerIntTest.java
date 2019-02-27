package com.icthh.xm.ms.entity.service.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.scheduler.domain.ScheduledEvent;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.service.FunctionExecutorService;
import com.icthh.xm.ms.entity.service.SchedulerHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
public class SchedulerIntTest {

    @Autowired
    private FunctionExecutorService functionService;


    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private SchedulerHandler schedulerHandler;

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    void initLeps() {
        leps.onRefresh("/config/tenants/RESINTTEST/entity/lep/scheduler/SchedulerEvent$$TEST_TYPE_KEY$$around.groovy",
            loadFile("config/testlep/SchedulerEvent$$TEST_TYPE_KEY$$around.groovy"));
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    @SneakyThrows
    public void testCallLepOnEvent() {
        initLeps();
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setTypeKey("TEST_TYPE_KEY");
        scheduledEvent.setKey(UUID.randomUUID().toString());
        Map<String, Object> data =  new HashMap<>();
        data.put("countCallEventHandler", 0);
        scheduledEvent.setData(data);
        schedulerHandler.onEvent(scheduledEvent, "RESINTTEST");

        assertThat(scheduledEvent.getData().get("countCallEventHandler")).isEqualTo(1);
    }

    @Test
    public void testOtherTenantOnEvent() {
        initLeps();
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setTypeKey("TEST_TYPE_KEY");
        scheduledEvent.setKey(UUID.randomUUID().toString());
        Map<String, Object> data =  new HashMap<>();
        data.put("countCallEventHandler", 0);
        scheduledEvent.setData(data);
        schedulerHandler.onEvent(scheduledEvent, "TEST");

        assertThat(scheduledEvent.getData().get("countCallEventHandler")).isEqualTo(0);
        assertThat(scheduledEvent.getData().get("scheduledEvent")).isNull();
    }

    @Test
    public void testOtherTypeKeyOnEvent() {
        initLeps();
        ScheduledEvent scheduledEvent = new ScheduledEvent();
        scheduledEvent.setTypeKey("OTHER_TEST_TYPE_KEY");
        scheduledEvent.setKey(UUID.randomUUID().toString());
        Map<String, Object> data =  new HashMap<>();
        data.put("countCallEventHandler", 0);
        scheduledEvent.setData(data);
        schedulerHandler.onEvent(scheduledEvent, "RESINTTEST");

        assertThat(scheduledEvent.getData().get("countCallEventHandler")).isEqualTo(0);
        assertThat(scheduledEvent.getData().get("scheduledEvent")).isNull();
    }

}
