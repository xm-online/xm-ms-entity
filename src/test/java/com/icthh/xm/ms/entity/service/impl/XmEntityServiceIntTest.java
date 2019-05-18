package com.icthh.xm.ms.entity.service.impl;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;

@Slf4j
public class XmEntityServiceIntTest extends AbstractSpringBootTest {

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntityServiceImpl xmEntityService;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private XmLepScriptConfigServerResourceLoader leps;

    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;

    @Autowired
    private MappingConfiguration mappingConfiguration;

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

    private XmEntity createXmEntity() {
        XmEntity xmEntity = new XmEntity().key(randomUUID().toString()).typeKey("TEST_DELETE");
        xmEntity.name("name")
            .functionContexts(asSet(
                new FunctionContext().key("1").typeKey("A").xmEntity(xmEntity),
                new FunctionContext().key("2").typeKey("A").xmEntity(xmEntity),
                new FunctionContext().key("3").typeKey("A").xmEntity(xmEntity)
            ))
            .attachments(asSet(
                new Attachment().typeKey("A").name("1"),
                new Attachment().typeKey("A").name("2"),
                new Attachment().typeKey("A").name("3")
            ))
            .calendars(asSet(
                new Calendar().typeKey("A").name("1").events(asSet(
                    new Event().typeKey("A").title("1"),
                    new Event().typeKey("A").title("2")
                )),
                new Calendar().typeKey("A").name("2").events(asSet(
                    new Event().typeKey("A").title("3"),
                    new Event().typeKey("A").title("4")
                ))
            ))
            .locations(asSet(
                new Location().typeKey("A").name("1"),
                new Location().typeKey("A").name("2")
            ))
            .ratings(asSet(
                new Rating().typeKey("A").votes(asSet(
                    new Vote().message("1").value(1.1).userKey("1"),
                    new Vote().message("2").value(2.1).userKey("2")
                ))
            ))
            .tags(asSet(
                new Tag().typeKey("A").name("1"),
                new Tag().typeKey("A").name("2")
            ))
            .comments(asSet(
                new Comment().message("1").userKey("1"),
                new Comment().message("2").userKey("1")
            ));
        return xmEntity;
    }

    @Test
    public void testDelete() {

        XmEntity entity = xmEntityService.save(createXmEntity());

        xmEntityService.delete(entity.getId());
    }

    @Test
    public void testDeleteLink() {

        XmEntity entity = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_DELETE"));
        XmEntity breakLink = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_SEARCH"));
        XmEntity cascadeDeleteLink = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_NEW"));

        Link link = new Link();
        entity.addTargets(link);

        link.setTypeKey("breakLinks");
        link.setTarget(breakLink);
        link.setSource(entity);

        Link link2 = new Link();
        link2.setTypeKey("cascadeDeleteLinks");
        link2.setTarget(cascadeDeleteLink);
        entity.addTargets(link2);

        xmEntityService.save(entity);

        XmEntity cascadeBreakSubLinks = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_SEARCH"));
        XmEntity cascadeDeleteSubLinks = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_NEW"));

        cascadeDeleteLink.addTargets(new Link().typeKey("cascadeBreakSubLinks").target(cascadeBreakSubLinks));
        cascadeDeleteLink.addTargets(new Link().typeKey("cascadeDeleteSubLinks").target(cascadeDeleteSubLinks));

        xmEntityService.save(cascadeDeleteLink);

        xmEntityService.delete(entity.getId());

        assertThat(xmEntityRepository.existsById(entity.getId())).isFalse();

        assertThat(xmEntityRepository.existsById(breakLink.getId())).isTrue();
        assertThat(xmEntityRepository.existsById(cascadeDeleteLink.getId())).isFalse();

        assertThat(xmEntityRepository.existsById(cascadeBreakSubLinks.getId())).isTrue();
        assertThat(xmEntityRepository.existsById(cascadeDeleteSubLinks.getId())).isFalse();

        xmEntityService.delete(breakLink.getId());
        xmEntityService.delete(cascadeBreakSubLinks.getId());
    }


    private <T> Set<T> asSet(T... elements) {
        Set<T> set = new HashSet<>();
        for(T element: elements) {
            set.add(element);
        }
        return set;
    }


    @Test
    @SneakyThrows
    public void testDeleteLinkInner() {

        XmEntity parentEntity = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_DELETE_PARENT"));

        XmEntity entity = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_DELETE"));
        XmEntity breakLink = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_SEARCH"));
        XmEntity cascadeDeleteLink = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_NEW"));


        Link testLink = new Link();
        parentEntity.addTargets(testLink);

        testLink.setTypeKey("testLink");
        testLink.setTarget(entity);
        testLink.setSource(parentEntity);
        xmEntityService.save(parentEntity);

        Link link = new Link();
        entity.addTargets(link);

        link.setTypeKey("breakLinks");
        link.setTarget(breakLink);
        link.setSource(entity);

        Link link2 = new Link();
        link2.setTypeKey("cascadeDeleteLinks");
        link2.setTarget(cascadeDeleteLink);
        entity.addTargets(link2);

        xmEntityService.save(entity);

        XmEntity cascadeBreakSubLinks = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_SEARCH"));
        XmEntity cascadeDeleteSubLinks = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_LIFECYCLE_LINK_NEW"));

        cascadeDeleteLink.addTargets(new Link().typeKey("cascadeBreakSubLinks").target(cascadeBreakSubLinks));
        cascadeDeleteLink.addTargets(new Link().typeKey("cascadeDeleteSubLinks").target(cascadeDeleteSubLinks));

        xmEntityService.save(cascadeDeleteLink);

        xmEntityService.delete(entity.getId());

        assertThat(xmEntityRepository.existsById(parentEntity.getId())).isTrue();

        assertThat(xmEntityRepository.existsById(entity.getId())).isFalse();

        assertThat(xmEntityRepository.existsById(breakLink.getId())).isTrue();
        assertThat(xmEntityRepository.existsById(cascadeDeleteLink.getId())).isFalse();

        assertThat(xmEntityRepository.existsById(cascadeBreakSubLinks.getId())).isTrue();
        assertThat(xmEntityRepository.existsById(cascadeDeleteSubLinks.getId())).isFalse();

        xmEntityService.delete(breakLink.getId());
        xmEntityService.delete(cascadeBreakSubLinks.getId());
        xmEntityService.delete(parentEntity.getId());
    }

    @Test
    @SneakyThrows
    public void testDeleteLinkDifferentLinkTypes() {

        XmEntity deletedEntity = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_DELETE_NEW_LINK"));
        XmEntity otherEntity = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TEST_DELETE_SEARCH_LINK"));
        XmEntity sharedEntity = xmEntityService.save(new XmEntity().name(" ").key(randomUUID()).typeKey("TARGET_ENTITY"));

        Link newLink = new Link();
        newLink.setTypeKey("newLink");
        newLink.setTarget(sharedEntity);
        deletedEntity.addTargets(newLink);

        Link searchLink = new Link();
        searchLink.setTypeKey("cascadeDeleteLinks");
        searchLink.setTarget(sharedEntity);
        otherEntity.addTargets(searchLink);

        xmEntityService.save(deletedEntity);
        xmEntityService.save(otherEntity);

        xmEntityService.delete(deletedEntity.getId());

        assertThat(xmEntityRepository.existsById(otherEntity.getId())).isTrue();
        assertThat(xmEntityRepository.existsById(sharedEntity.getId())).isTrue();
        assertThat(xmEntityRepository.existsById(deletedEntity.getId())).isFalse();

        xmEntityService.delete(sharedEntity.getId());
        xmEntityService.delete(otherEntity.getId());
    }


    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testJpql() throws Exception {

        // Initialize the database
        XmEntity entity1 = new XmEntity().typeKey("ENTITY1").name("###1").key(randomUUID());
        xmEntityService.save(entity1);
        XmEntity entity2 = new XmEntity().typeKey("ENTITY1").name("###2").key(randomUUID());
        xmEntityService.save(entity2);
        xmEntityService.save(new XmEntity().typeKey("ENTITY2").name("###3").key(randomUUID()));
        xmEntityService.save(new XmEntity().typeKey("ACCOUNTING").name("###4").key(randomUUID()));

        List<XmEntity> entities = xmEntityService.findAll("Select entity from XmEntity entity where entity.typeKey = :typeKey", of("typeKey", "ENTITY1"), asList("data"));
        log.info("Entities {}", entities);

        assertEquals(entities, asList(entity1, entity2));

    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testAdditionalMapping() {
        elasticsearchIndexService.reindexAll();

        Map<String, Object> xmEntityData = new HashMap<>();
        xmEntityData.put("targetField", "C-D");
        xmEntityData.put("notSaveField", "FF-AA");

        XmEntity entity1 = new XmEntity().typeKey("TEST_SEARCH").name("A-B").key("E-F")
            .data(xmEntityData);
        XmEntity entity2 = new XmEntity().typeKey("TEST_SEARCH").name("B-A").key("F-E")
            .data(of("targetField", "D-C"));
        xmEntityService.save(entity1);
        xmEntityService.save(entity2);
        PageRequest page = PageRequest.of(0, 10, Sort.by("key"));
        Page<XmEntity> search = xmEntityService.search("data.targetField: C-D", page, null);
        assertEquals(search.getContent(), asList(entity1, entity2));
        Page<XmEntity> searchByKey = xmEntityService.search("key: F-E", page, null);
        assertEquals(searchByKey.getContent(), asList(entity2));
        Page<XmEntity> searchByKeyword = xmEntityService.search("data.targetField.keyword: C-D", page, null);
        assertEquals(searchByKeyword.getContent(), asList(entity1));

        reindexWithMapping("config/test-mapping.json");
        Page<XmEntity> searchWithMapping = xmEntityService.search("data.targetField: C-D", page, null);
        assertEquals(searchWithMapping.getContent(), asList(entity1));
        Page<XmEntity> searchByKeyWithMapping = xmEntityService.search("key: F-E", page, null);
        assertNotEquals(searchByKeyWithMapping.getContent(), asList(entity2));

        reindexWithMapping("config/test-mapping-with-not-analyzed-key.json");
        searchWithMapping = xmEntityService.search("data.targetField: C-D", page, null);
        assertEquals(searchWithMapping.getContent(), asList(entity1));
        searchByKeyWithMapping = xmEntityService.search("key.keyword: F-E", page, null);
        assertEquals(searchByKeyWithMapping.getContent(), asList(entity2));

        reindexWithMapping("config/test-mapping-with-not-save-field.json");
        searchWithMapping = xmEntityService.search("data.targetField: C-D", page, null);
        assertNotEquals(searchWithMapping.getContent(), asList(entity1));
        xmEntityData.remove("notSaveField");
        assertEquals(searchWithMapping.getContent().get(0).getData(), xmEntityData);
    }

    private void reindexWithMapping(String mappingPath) {
        String mapping = loadFile(mappingPath);
        mappingConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/mapping.json", mapping);
        elasticsearchIndexService.reindexAll();
    }

    @SneakyThrows
    public static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

    @Test
    public void ifGlobalTransactionThrowExceptionSeparateTransactionAlreadyCommited() {
        class TestException extends RuntimeException{}
        List<Long> ids = new ArrayList<>();
        String inSeparateTransaction = "inSeparateTransaction";
        String inGlobalTransaction = "inGlobalTransaction";

        try {
            transactionExecutor.doInSeparateTransaction(() -> {
                ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                           .typeKey("TARGET_ENTITY")).getId());
                ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                           .typeKey("TARGET_ENTITY")).getId());
                transactionExecutor.doInSeparateTransaction(() -> {
                    ids.add(xmEntityService.save(new XmEntity().name(inSeparateTransaction).key(randomUUID())
                                                               .typeKey("TARGET_ENTITY")).getId());
                    return null;
                });
                ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                           .typeKey("TARGET_ENTITY")).getId());
                throw new TestException();
            });
        } catch (TestException e) {
            log.info("All is ok");
        }
        List<XmEntity> allEntitis = xmEntityRepository.findAllById(ids);
        log.info("{}", allEntitis);
        assertEquals(allEntitis.size(), 1);
        allEntitis.forEach(it -> assertEquals(it.getName(), inSeparateTransaction));

    }


    @Test
    public void ifSeparateTransactionThrowExceptionGlobalTransactionWillSuccessCommit() {
        class TestException extends RuntimeException{}
        List<Long> ids = new ArrayList<>();
        String inSeparateTransaction = "inSeparateTransaction";
        String inGlobalTransaction = "inGlobalTransaction";

        transactionExecutor.doInSeparateTransaction(() -> {
            ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                       .typeKey("TARGET_ENTITY")).getId());
            ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                       .typeKey("TARGET_ENTITY")).getId());
            try {
                transactionExecutor.doInSeparateTransaction(() -> {
                    ids.add(xmEntityService.save(new XmEntity().name(inSeparateTransaction).key(randomUUID())
                                                               .typeKey("TARGET_ENTITY")).getId());
                    throw new TestException();
                });
            } catch (TestException e) {
                log.info("All is ok");
            }
            ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                       .typeKey("TARGET_ENTITY")).getId());
            return null;
        });

        List<XmEntity> allEntitis = xmEntityRepository.findAllById(ids);
        log.info("{}", allEntitis);
        assertEquals(allEntitis.size(), 3);
        allEntitis.forEach(it -> assertEquals(it.getName(), inGlobalTransaction));
    }

    @Test
    public void testDoInSeparateTransaction() {
        List<Long> ids = new ArrayList<>();
        String inSeparateTransaction = "inSeparateTransaction";
        String inGlobalTransaction = "inGlobalTransaction";

        transactionExecutor.doInSeparateTransaction(() -> {
            ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                       .typeKey("TARGET_ENTITY")).getId());
            ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                       .typeKey("TARGET_ENTITY")).getId());
            transactionExecutor.doInSeparateTransaction(() -> {
                ids.add(xmEntityService.save(new XmEntity().name(inSeparateTransaction).key(randomUUID())
                                                           .typeKey("TARGET_ENTITY")).getId());
                return null;
            });
            ids.add(xmEntityService.save(new XmEntity().name(inGlobalTransaction).key(randomUUID())
                                                       .typeKey("TARGET_ENTITY")).getId());
            return null;
        });
        List<XmEntity> allEntitis = xmEntityRepository.findAllById(ids);
        log.info("{}", allEntitis);
        assertEquals(allEntitis.size(), 4);
    }

}
