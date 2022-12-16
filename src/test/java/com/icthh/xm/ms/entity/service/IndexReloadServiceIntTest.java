package com.icthh.xm.ms.entity.service;

import com.ecwid.consul.transport.RawResponse;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.topic.service.KafkaTemplateService;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.kafka.SystemEvent;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.service.mapper.XmEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ContextConfiguration(classes = {IndexReloadServiceIntTest.ConsulConfiguration.class})
public class IndexReloadServiceIntTest extends AbstractSpringBootTest {

    public static final String DEFAULT_TENANT = "RESINTTEST";
    private static final String USER_TYPE_KEY = "ACCOUNT.USER";
    private static final String TEST_TYPE_KEY = "TEST_LIFECYCLE";

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private XmEntityMapper xmEntityMapper;

    @Mock
    private KafkaTemplateService kafkaTemplateService;

    @Mock
    private ConsulClient consulClient;

    private IndexReloadService indexReloadService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, DEFAULT_TENANT);

        MockitoAnnotations.initMocks(this);

        indexReloadService = new IndexReloadService(consulClient, kafkaTemplateService, xmEntityRepository, objectMapper, xmEntityMapper);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @Before
    public void before() {
        xmEntityRepository.save(createEntity(1L, USER_TYPE_KEY));
        xmEntityRepository.save(createEntity(2L, TEST_TYPE_KEY));
    }

    @After
    public void after() {
        xmEntityRepository.deleteAll();

        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }


    @Test
    @Transactional
    public void reloadData() {
        when(consulClient.setKVValue(anyString(), anyString(), any(PutParams.class)))
            .thenReturn(new Response<>(true, new RawResponse(200, null, null,
                null, null, null)));

        SystemEvent event = createEvent();
        indexReloadService.reloadData(event);

        String dbTopic = "event." + DEFAULT_TENANT + ".db";
        verify(kafkaTemplateService, times(1)).send(eq(dbTopic), anyString());
    }

    private SystemEvent createEvent() {
        SystemEvent event = new SystemEvent();
        event.setData(Map.of("typeKey", USER_TYPE_KEY));
        event.setTenantKey(DEFAULT_TENANT);
        return event;
    }

    private XmEntity createEntity(Long id, String typeKey) {
        XmEntity entity = new XmEntity();
        entity.setId(id);
        entity.setName("Name");
        entity.setTypeKey(typeKey);
        entity.setStartDate(new Date().toInstant());
        entity.setUpdateDate(new Date().toInstant());
        entity.setKey("KEY-" + id);
        entity.setStateKey("STATE1");
        entity.setData(ImmutableMap.<String, Object>builder()
            .put("AAAAAAAAAA", "BBBBBBBBBB").build());
        return entity;
    }

    @TestConfiguration
    static class ConsulConfiguration {
        @Bean
        public ConsulClient consulClient() {
            return new ConsulClient();
        }
    }
}
