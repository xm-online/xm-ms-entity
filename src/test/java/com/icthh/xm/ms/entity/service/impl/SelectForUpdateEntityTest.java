package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    TenantContextConfiguration.class
})
public class SelectForUpdateEntityTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager em;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Before
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }


    @After
    public void afterTest() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    //@Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void findOneForUpdate() throws InterruptedException {
        XmEntity entity = createEntity(25l, "ACCOUNT");
        XmEntity sourceEntity = xmEntityRepository.save(entity);
        log.info("Saved: {}", IdOrKey.of(sourceEntity.getId()));
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
            lepManager.beginThreadContext(ctx -> {
                ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
                ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
            });
            XmEntity entity1 = xmEntityService.selectAndUpdate(IdOrKey.of(sourceEntity.getId()), first -> {
                assertEquals("initial", first.getData().get("AAAAAAAAAA"));
                assertEquals("Initial", first.getName());
                first.setData(of("AAAAAAAAAA", "first"));
                first.setName("First");
                for (int i = 0; i < 10; i++) {
                    log.info("Waiting .... {} sec", (i + 1) * 1000);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            log.info("First: {}", entity1);
        });

        Thread.sleep(1000);

        executorService.submit(() -> {
            TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
            lepManager.beginThreadContext(ctx -> {
                ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
                ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
            });
            XmEntity entity2 = xmEntityService.selectAndUpdate(IdOrKey.of(sourceEntity.getId()), second -> {
                assertEquals("first", second.getData().get("AAAAAAAAAA"));
                assertEquals("First", second.getName());
                second.setData(of("AAAAAAAAAA", "second"));
                second.setName("Second");
            });
            log.info("Second: {}", entity2);
        });

        executorService.awaitTermination(12, TimeUnit.SECONDS);

        XmEntity after = xmEntityService.findOne(IdOrKey.of(sourceEntity.getId()));
        assertEquals("second", after.getData().get("AAAAAAAAAA"));
        assertEquals("Second", after.getName());
    }

    private XmEntity createEntity(Long id, String typeKey) {
        XmEntity entity = new XmEntity();
        entity.setId(id);
        entity.setName("Initial");
        entity.setTypeKey(typeKey);
        entity.setStartDate(new Date().toInstant());
        entity.setUpdateDate(new Date().toInstant());
        entity.setKey("KEY-" + id);
        entity.setStateKey("STATE1");
        entity.setData(of("AAAAAAAAAA", "initial"));
        return entity;
    }
}
