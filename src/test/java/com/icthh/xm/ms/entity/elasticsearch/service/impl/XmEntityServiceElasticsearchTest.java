package com.icthh.xm.ms.entity.elasticsearch.service.impl;

import com.icthh.xm.commons.lep.XmLepScriptConfigServerResourceLoader;
import com.icthh.xm.commons.search.ElasticsearchException;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
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
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.elasticsearch.AbstractElasticSpringBootTest;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import com.icthh.xm.ms.entity.service.SeparateTransactionExecutor;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@Slf4j
public class XmEntityServiceElasticsearchTest extends AbstractElasticSpringBootTest {

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

    @Autowired
    private IndexConfiguration indexConfiguration;

    @Autowired
    private XmEntityTenantConfigService xmEntityTenantConfigService;

    @Autowired
    private EntityManager entityManager;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    private List<String> lepsForCleanUp = new ArrayList<>();

    @BeforeEach
    public void before() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getRequiredUserKey()).thenReturn("userKey");

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        String pattern = "/config/tenants/RESINTTEST/entity/lep/service/entity/";
        addLep(pattern, "TEST_LIFECYCLE_TYPE_KEY");
        addLep(pattern, "TEST_LIFECYCLE_TYPE_KEY$SUB");
        addLep(pattern, "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD");
        addLep(pattern, "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD$SUBCHILD");
        addLep(pattern, "TEST_LIFECYCLE_TYPE_KEY$SUB$CHILD$SUBCHILD$NEXTCHILD");
    }


    @AfterEach
    public void afterTest() {
        lepsForCleanUp.forEach(it -> leps.onRefresh(it, null));
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    private void addLep(String pattern, String lepName) {
        String lepBody = loadFile("config/testlep/Save$$TEST_LIFECYCLE_TYPE_KEY$$around.groovy");
        lepBody = StrSubstitutor.replace(lepBody, of("lepName", lepName));
        leps.onRefresh(pattern + "Save$$" + lepName + "$$around.groovy", lepBody);
        lepsForCleanUp.add(pattern + "Save$$" + lepName + "$$around.groovy");
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

    private <T> Set<T> asSet(T... elements) {
        Set<T> set = new HashSet<>();
        for(T element: elements) {
            set.add(element);
        }
        return set;
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testAdditionalMapping() {


        Map<String, Object> xmEntityData = new HashMap<>();
        xmEntityData.put("targetField", "C-D");
        xmEntityData.put("notSaveField", "FF-AA");

        XmEntity entity1 = new XmEntity().typeKey("TEST_SEARCH").name("A-B").key("E-F")
            .data(xmEntityData);
        XmEntity entity2 = new XmEntity().typeKey("TEST_SEARCH").name("B-A").key("F-E")
            .data(of("targetField", "D-C"));
        xmEntityService.save(entity1);
        xmEntityService.save(entity2);

        elasticsearchIndexService.reindexAll();

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

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testIndexConfiguration() {
        elasticsearchIndexService.reindexAll();

        Map<String, Object> xmEntityData = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            xmEntityData.put("key-" + i, "value-" + i);
        }
        XmEntity entity = new XmEntity().typeKey("TEST_SEARCH")
                                        .name("A-B")
                                        .key("E-F")
                                        .data(xmEntityData);
        entity = xmEntityService.save(entity);

        Map<String, String> elasticFailedDocument = null;

        try {
            elasticsearchIndexService.reindexAll();
        } catch (ElasticsearchException e) {
            elasticFailedDocument = e.getFailedDocuments();
        }

        assertNotNull(elasticFailedDocument);
        assertEquals(entity.getId().toString(), elasticFailedDocument.entrySet().iterator().next().getKey());

        String config = loadFile("config/elastic_config.json");
        indexConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/index_config.json", config);
        xmEntityService.delete(entity.getId());
        elasticsearchIndexService.reindexAll();
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

//    TODO-IMPL
//    @Test(expected = SearchPhaseExecutionException.class)
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchFailWithMaxResultWindow1000() {
        elasticsearchIndexService.reindexAll();

        saveXmEntities(2);

        String config = loadFile("config/elastic_config_window1000.json");
        indexConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/index_config.json", config);
        elasticsearchIndexService.reindexAll();

        Page<XmEntity> pageResult = xmEntityService.search("typeKey:TEST_SEARCH", PageRequest.of(0, 1001), null);
        List<XmEntity> zeroPageContent = pageResult.getContent();
        assertThat(pageResult.getTotalPages()).isEqualTo(1);
        assertThat(zeroPageContent.size()).isEqualTo(2);

        indexConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/index_config.json", null);
        elasticsearchIndexService.reindexAll();
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testSearchWithScroll() {
        elasticsearchIndexService.reindexAll();

        saveXmEntities(2);

        String config = loadFile("config/elastic_config.json");
        indexConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/index_config.json", config);
        elasticsearchIndexService.reindexAll();

        Page<XmEntity> scrollResult = xmEntityService.search(
            50L,
            "typeKey:TEST_SEARCH",
            PageRequest.of(0, 1000),
            null);
        assertThat(scrollResult.getTotalElements()).isEqualTo(2);
        assertThat(scrollResult.getContent().size()).isEqualTo(2);
        assertThat(scrollResult.getTotalPages()).isEqualTo(1);

        indexConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/index_config.json", null);
    }

    @Test
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void testFindLink() {

        List<XmEntity> entities = transactionExecutor.doInSeparateTransaction(() -> {
            XmEntity entity1 = xmEntityService.save(new XmEntity().typeKey("TEST_SEARCH").key("key1").name("name"));
            XmEntity entity2 = xmEntityService.save(new XmEntity().typeKey("TEST_SEARCH").key("key2").name("name"));
            XmEntity entity3 = xmEntityService.save(new XmEntity().typeKey("TEST_SEARCH").key("key3").name("name"));
            XmEntity entity4 = xmEntityService.save(new XmEntity().typeKey("TEST_SEARCH").key("key4").name("name"));
            XmEntity entity5 = xmEntityService.save(new XmEntity().typeKey("TEST_SEARCH").key("key5").name("name"));

            Link link1 = new Link().typeKey("TEST_SEARCH_LINK");
            entity1.addTargets(link1);
            link1.setTarget(entity2);
            link1.setStartDate(Instant.now());

            Link link2 = new Link().typeKey("TEST_SEARCH_LINK");
            entity1.addTargets(link2);
            link2.setTarget(entity3);
            link2.setStartDate(Instant.now());

            xmEntityService.save(entity1);
            xmEntityService.save(entity2);
            xmEntityService.save(entity3);

            return asList(entity1, entity2, entity3, entity4, entity5);
        });

        elasticsearchIndexService.reindexAll();

        List<XmEntity> result = xmEntityService.searchXmEntitiesToLink(IdOrKey.of(entities.get(0).getId()),
            "TEST_SEARCH", "TEST_SEARCH_LINK", "name: name",
            PageRequest.of(0, 10), null).getContent();

        assertThat(result).contains(entities.get(3), entities.get(4));
        assertThat(result).doesNotContain(entities.get(0), entities.get(1), entities.get(2));
        entities.forEach(it -> xmEntityService.delete(it.getId()));
    }

    private List<XmEntity> saveXmEntities(int count) {
        Map<String, Object> xmEntityData = new HashMap<>();
        xmEntityData.put("key", "value");
        List<XmEntity> entityList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            XmEntity entity = new XmEntity().typeKey("TEST_SEARCH")
                .name("A-B" + i)
                .key("E-F" + i)
                .data(xmEntityData)
                .startDate(new Date().toInstant())
                .updateDate(new Date().toInstant());
            entityList.add(entity);
        }
        return xmEntityRepository.saveAll(entityList);
    }

}
