package com.icthh.xm.ms.entity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;

import com.icthh.xm.commons.lep.api.LepManagementService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.ElasticIndexManagerService;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("no-elastic")
public class XmEntityNoElasticIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private XmEntityService xmEntityService;

    @MockBean
    private ElasticIndexManagerService elasticIndexManagerService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManagementService lepManagementService;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;


    @BeforeEach
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManagementService.beginThreadContext();
    }

    @Test
    public void testSaveXmEntityUsesNoOpStubs() {
        XmEntity entity = new XmEntity()
            .typeKey("TEST_NO_PROCESSING_REFS")
            .name("Test Entity")
            .key(UUID.randomUUID().toString())
            .startDate(new Date().toInstant());

        XmEntity savedEntity = xmEntityService.save(entity);

        assertNotNull(savedEntity.getId());
        assertEquals("Test Entity", savedEntity.getName());
        verifyNoInteractions(elasticIndexManagerService);
    }

    @Test
    public void testUpdateXmEntityUsesNoOpStubs() {
        XmEntity entity = new XmEntity()
            .typeKey("TEST_NO_PROCESSING_REFS")
            .name("Original Name")
            .key(UUID.randomUUID().toString());

        XmEntity savedEntity = xmEntityService.save(entity);

        savedEntity.setName("Updated Name");
        XmEntity updatedEntity = xmEntityService.save(savedEntity);

        assertEquals("Updated Name", updatedEntity.getName());

        verifyNoInteractions(elasticIndexManagerService);
    }
}
