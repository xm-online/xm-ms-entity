package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

@Transactional
@Slf4j
public class AttachmentRepositoryIntTest extends AbstractSpringBootTest {

    @PersistenceContext
    public EntityManager entityManager;

    private Session session;

    private Statistics statistics;

    @Autowired
    XmEntityRepository xmEntityRepository;

    @Autowired
    AttachmentRepository attachmentRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Autowired
    private SeparateTransactionExecutor separateTransactionExecutor;

    public static String ATTACHMENT_TYPE_KEY = "IMAGE";
    public static String ENTITY_TYPE_KEY = "TYPE1";

    @Before
    public void init() {
        session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.clear();
    }

    @Before
    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    @Transactional
    public void findAll() {
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            List<Attachment> attachments = List.of(
                createAttachment("Attachment 1", ATTACHMENT_TYPE_KEY, "111", createXmEntity()),
                createAttachment("Attachment 2", ATTACHMENT_TYPE_KEY, "222", createXmEntity())
            );
            attachmentRepository.saveAll(attachments);
            return null;
        });
        statistics.clear();

        attachmentRepository.findAll(Specification.where((root, query, cb) -> {
            return cb.equal(root.get("typeKey"), ATTACHMENT_TYPE_KEY);
        }), PageRequest.of(0, 10));

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    private Attachment createAttachment(String name, String typeKey, String contentValue, XmEntity xmEntity) {
        Attachment attachment = new Attachment();
        attachment.name(name);
        attachment.typeKey(typeKey);
        Content content = new Content();
        content.setValue(contentValue.getBytes());
        attachment.content(content);
        attachment.setXmEntity(xmEntity);
        return attachment;
    }

    private XmEntity createXmEntity() {
        XmEntity xmEntity = new XmEntity()
            .typeKey(ENTITY_TYPE_KEY)
            .key(randomUUID())
            .name("name")
            .startDate(now())
            .updateDate(now());
        return xmEntityRepository.save(xmEntity);
    }
}
