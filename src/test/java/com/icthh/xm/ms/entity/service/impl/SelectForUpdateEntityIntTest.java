package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SelectForUpdateEntityIntTest extends AbstractSpringBootTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private XmEntityRepository xmEntityRepository;

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

    /**
     * This test requires that LOCK_TIMEOUT in DB will be more than 10 seconds.
     *
     * In H2 default value is 1 second (http://h2database.com/html/grammar.html#set_lock_timeout)
     *
     * <p/> Timeout can be set in application.yml config as:
     *
     * <pre>
     *  spring:
     *     datasource:
     *         url: jdbc:h2:mem:entity;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000
     * </pre>
     *
     * @throws InterruptedException
     */
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
                for (int i = 0; i < 3; i++) {
                    log.info("Waiting .... {} milliseconds", (i + 1) * 500);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            log.info("First: {}", entity1);
        });

        Thread.sleep(500);

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

        executorService.awaitTermination(3000, TimeUnit.MILLISECONDS);

        XmEntity after = xmEntityService.findOne(IdOrKey.of(sourceEntity.getId()));
        assertEquals("second", after.getData().get("AAAAAAAAAA"));
        assertEquals("Second", after.getName());
    }

    private XmEntity createEntity(Long id, String typeKey) {
        XmEntity entity = new XmEntity();
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
