package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

@Transactional
@Slf4j
public class LinkRepositoryIntTest extends AbstractSpringBootTest {

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    XmEntityRepository xmEntityRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @PersistenceContext
    public EntityManager entityManager;

    private Statistics statistics;

    private Session session;

    @Autowired
    private SeparateTransactionExecutor separateTransactionExecutor;

    private static final String LINK_TYPE_KEY = "LINK1";
    private static final String ENTITY_TYPE_KEY1 = "TYPE1";
    private static final String ENTITY_TYPE_KEY2 = "TYPE2";

    @Before
    public void setUp() {
        session = entityManager.unwrap(Session.class);
        statistics = session.getSessionFactory().getStatistics();
        statistics.clear();
    }

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
    public void findBySourceIdAndTargetTypeKey() {
        Long sourceId = separateTransactionExecutor.doInSeparateTransaction(() -> {
            XmEntity source = createXmEntity(ENTITY_TYPE_KEY1);
            List<Link> links = List.of(
                createLink(source, createXmEntity(ENTITY_TYPE_KEY2)),
                createLink(source, createXmEntity(ENTITY_TYPE_KEY2))
            );
            linkRepository.saveAll(links);
            return source.getId();
        });
        statistics.clear();

        linkRepository.findBySourceIdAndTargetTypeKey(sourceId, ENTITY_TYPE_KEY2);

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    public void findByTargetIdAndTypeKey() {
        Long targetId = separateTransactionExecutor.doInSeparateTransaction(() -> {
            XmEntity target = createXmEntity(ENTITY_TYPE_KEY2);
            List<Link> links = List.of(
                createLink(createXmEntity(ENTITY_TYPE_KEY1), target),
                createLink(createXmEntity(ENTITY_TYPE_KEY1), target)
            );
            linkRepository.saveAll(links);
            return target.getId();
        });
        statistics.clear();

        linkRepository.findByTargetIdAndTypeKey(targetId, LINK_TYPE_KEY);

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    public void findBySourceTypeKeyAndTypeKeyIn() {
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            List<Link> links = List.of(
                createLink(createXmEntity(ENTITY_TYPE_KEY1), createXmEntity(ENTITY_TYPE_KEY2)),
                createLink(createXmEntity(ENTITY_TYPE_KEY1), createXmEntity(ENTITY_TYPE_KEY2))
            );
            linkRepository.saveAll(links);
            return null;
        });
        statistics.clear();

        linkRepository.findBySourceTypeKeyAndTypeKeyIn(ENTITY_TYPE_KEY1, List.of(LINK_TYPE_KEY));

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    public void findBySourceIdAndTypeKey() {
        Long sourceId = separateTransactionExecutor.doInSeparateTransaction(() -> {
            XmEntity source = createXmEntity(ENTITY_TYPE_KEY1);
            List<Link> links = List.of(
                createLink(source, createXmEntity(ENTITY_TYPE_KEY2)),
                createLink(source, createXmEntity(ENTITY_TYPE_KEY2))
            );
            linkRepository.saveAll(links);
            return source.getId();
        });
        statistics.clear();

        linkRepository.findBySourceIdAndTypeKey(sourceId, LINK_TYPE_KEY);

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Transactional
    @Test
    public void findAll() throws JsonProcessingException {
        separateTransactionExecutor.doInSeparateTransaction(() -> {
            List<Link> links = List.of(
                createLink(createXmEntity(ENTITY_TYPE_KEY1), createXmEntity(ENTITY_TYPE_KEY2)),
                createLink(createXmEntity(ENTITY_TYPE_KEY1), createXmEntity(ENTITY_TYPE_KEY2))
            );
            linkRepository.saveAll(links);
            return null;
        });
        statistics.clear();

        linkRepository.findAll(Specification.where((root, query, cb) -> {
            return cb.equal(root.get("typeKey"), LINK_TYPE_KEY);
        }));

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    private Link createLink(XmEntity source, XmEntity target) {
        return new Link().typeKey(LINK_TYPE_KEY)
            .startDate(Instant.now())
            .source(source)
            .target(target);
    }

    private XmEntity createXmEntity(String typeKey) {
        XmEntity xmEntity = new XmEntity()
            .typeKey(typeKey)
            .key(randomUUID())
            .name("name")
            .startDate(now())
            .updateDate(now());
        return xmEntityRepository.save(xmEntity);
    }
}
