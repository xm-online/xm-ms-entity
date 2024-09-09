package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Attachment_;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;


import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.web.rest.AttachmentResourceExtendedIntTest.createEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@Slf4j
public class AttachmentServiceIntTest extends AbstractSpringBootTest {

    public static final String TEST_ATTACHMENT = "A3";

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private EntityManager em;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

    }

    @After
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }


    @Test
    @Transactional
    public void testFindAll() {
        mockData();
        Attachment attachment = createEntity(em).contentUrl(TEST_ATTACHMENT);
        attachmentService.save(attachment);
        var result = attachmentService.findAll(Specification.where((root, query, builder) ->
                builder.equal(root.get(Attachment_.contentUrl), TEST_ATTACHMENT)), PageRequest.of(0, 10));
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(it -> assertEquals(TEST_ATTACHMENT, it.getContentUrl()));
    }

    private void mockData() {
        for (int i = 0; i < 10; i++) {
            Attachment attachment = createEntity(em).contentUrl("A" + i);
            attachmentService.save(attachment);
        }
    }

    @Test
    @Transactional
    public void testDeleteAll() {
        mockData();
        Attachment attachment = createEntity(em).contentUrl(TEST_ATTACHMENT);
        attachmentService.save(attachment);

        long size = attachmentRepository.count();
        var result = attachmentService.findAll(Specification.where((root, query, builder) ->
                builder.equal(root.get(Attachment_.contentUrl), TEST_ATTACHMENT)), PageRequest.of(0, 10));
        attachmentService.deleteAll(result.getContent());
        assertEquals(size - 2, attachmentRepository.count());
    }

}
