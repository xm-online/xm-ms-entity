package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class XmEntityBulkElasticSaveIntTest extends AbstractSpringBootTest {

    @MockBean
    private XmEntitySearchRepository searchRepository;
    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @Autowired
    private XmEntityServiceImpl xmEntityService;
    @Autowired
    private SeparateTransactionExecutor transactionExecutor;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;
    @Mock
    private XmAuthenticationContext context;

    @Before
    public void before() {
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
        lepManager.endThreadContext();
    }

    @Test
    public void testDoInSeparateTransaction() {
        List<XmEntity> savedInside = new ArrayList<>();
        List<XmEntity> savedGlobal = new ArrayList<>();
        MutableObject<List<XmEntity>> savedActual = new MutableObject<>();
        String inSeparateTransaction = "inSeparateTransaction";
        String inGlobalTransaction = "inGlobalTransaction";

        // argument captor and verify statement capture only object reference, but we need collection value to assert,
        // than erased by logic before activation finished
        when(searchRepository.saveAll(anyCollection())).then((Answer<List<XmEntity>>) invocation -> {
            List<XmEntity> entities = (List<XmEntity>) invocation.getArguments()[0];
            savedActual.setValue(new ArrayList<>(entities));
            return entities;
        });

        transactionExecutor.doInSeparateTransaction(() -> {
            savedGlobal.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                    .typeKey("TARGET_ENTITY")));
            savedGlobal.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                    .typeKey("TARGET_ENTITY")));
            transactionExecutor.doInSeparateTransaction(() -> {
                savedInside.add(xmEntityService.save(new XmEntity().name(inSeparateTransaction).key(randomUUID())
                        .typeKey("TARGET_ENTITY")));
                savedInside.add(xmEntityService.save(new XmEntity().name(inSeparateTransaction).key(randomUUID())
                        .typeKey("TARGET_ENTITY")));
                return null;
            });
            // any collection because clean up process clean collection, and test can not verify collection content
            verify(searchRepository).saveAll(anyCollection());
            assertEquals(savedInside, savedActual.getValue());
            savedGlobal.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                    .typeKey("TARGET_ENTITY")));
            return null;
        });
        // any collection because clean up process clean collection, and test can not verify collection content
        verify(searchRepository, times(2)).saveAll(anyCollection());
        assertEquals(savedGlobal, savedActual.getValue());
    }

}
