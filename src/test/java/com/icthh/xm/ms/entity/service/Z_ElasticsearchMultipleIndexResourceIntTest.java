package com.icthh.xm.ms.entity.service;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.web.rest.TestUtil.sameInstant;
import static com.icthh.xm.ms.entity.web.rest.XmEntityResourceExtendedIntTest.createEntity;
import static com.icthh.xm.ms.entity.web.rest.XmEntityResourceExtendedIntTest.createEntityComplexIncoming;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.Sets;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.elasticsearch.EmbeddedElasticsearchConfig;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.listener.XmEntityElasticSearchListener;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.XmEntityElasticRepository;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.XmEntityMultipleIndexElasticIndexRepository;
import com.icthh.xm.ms.entity.repository.search.elasticsearch.index.ElasticIndexNameResolver;
import com.icthh.xm.ms.entity.web.rest.ElasticsearchIndexResource;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test class for the ElasticsearchIndexResource REST controller and ElasticsearchIndexService service.
 *
 * @see ElasticsearchIndexResource
 * @see ElasticsearchIndexService
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
@ActiveProfiles("multipleIndex")
public class Z_ElasticsearchMultipleIndexResourceIntTest extends AbstractSpringBootTest  {


    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String ANOTHER_TYPE_KEY = "ACCOUNT.OWNER";
    private static final String DEFAULT_NAME = "AAAAAAAAAA";

    @Autowired
    ElasticIndexNameResolver elasticIndexNameResolver;

    protected static boolean elasticInited = false;

    protected MockMvc mockMvc;

    @Autowired
    protected XmEntityService xmEntityService;

    @Autowired
    protected XmEntityResource xmEntityResource;

    @Autowired
    protected MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    protected PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    protected ExceptionTranslator exceptionTranslator;

    @Autowired
    protected TenantContextHolder tenantContextHolder;

    @Autowired
    protected XmAuthenticationContextHolder authenticationContextHolder;

    @Autowired
    protected LepManager lepManager;

    @Autowired
    protected XmEntitySearchRepository searchRepository;

    @Autowired
    protected XmEntityRepositoryInternal xmEntityRepositoryInternal;

    @Autowired
    protected XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    protected ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    protected MappingConfiguration mappingConfiguration;

    @Autowired
    protected IndexConfiguration indexConfiguration;

    @Autowired
    protected XmEntityElasticSearchListener xmEntityElasticSearchListener;

    @Autowired
    protected XmEntitySpecService xmEntitySpecService;

    @Autowired
    protected SeparateTransactionExecutor transactionExecutor;

    @Autowired
    protected XmEntityElasticRepository xmEntityElasticRepository;

    protected ElasticsearchIndexService elasticsearchIndexService;

    @Mock
    protected Executor executor;

    @Mock
    protected XmEntitySpecService xmEntitySpecServiceMock;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authenticationContextHolder.getContext());
        });
        if (!elasticInited) {
            elasticsearchTemplate.deleteIndex(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));
            elasticsearchTemplate.createIndex(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));
            elasticInited = true;
        }

        xmEntityRepositoryInternal.deleteAll();
        cleanElasticsearch(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));


        elasticsearchIndexService = new ElasticsearchIndexService(xmEntityRepositoryInternal,
            xmEntitySearchRepository,
            elasticsearchTemplate,
            tenantContextHolder,
            mappingConfiguration,
            indexConfiguration,
            executor,
            xmEntityElasticRepository);

        elasticsearchIndexService.setSelfReference(elasticsearchIndexService);

        ElasticsearchIndexResource elasticsearchIndexResource =
            new ElasticsearchIndexResource(elasticsearchIndexService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(elasticsearchIndexResource, xmEntityResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        // make executor run task immediately
        doAnswer(a -> {
            ((Runnable) a.getArguments()[0]).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        xmEntityElasticSearchListener.setXmEntitySpecService(xmEntitySpecServiceMock);
    }

    @After
    public void after() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        xmEntityElasticSearchListener.setXmEntitySpecService(xmEntitySpecService);
    }

    @SneakyThrows
    @Test
    @Transactional
    public void reindex() {
        reindexWithRestApi();
    }

    @SneakyThrows
    @Test
    @Transactional
    public void reindexComplexEntity() {
        XmEntity saved = xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY));
        Tag tag = saved.getTags().iterator().next();
        Attachment attachment = saved.getAttachments().iterator().next();
        Location location = saved.getLocations().iterator().next();
        reindexWithRestApi();

        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        assert attachment.getXmEntity().getId() != null;
        assert location.getXmEntity().getId() != null;
        assert saved.getId() != null;
        assert tag.getXmEntity().getId() != null;

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?query=id: {id}" +
            "&typeKey="+DEFAULT_TYPE_KEY, saved.getId()))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$.[0].id").value(saved.getId()))
               .andExpect(jsonPath("$.[0].key").value(saved.getKey()))
               .andExpect(jsonPath("$.[0].typeKey").value(saved.getTypeKey()))
               .andExpect(jsonPath("$.[0].stateKey").value(saved.getStateKey()))
               .andExpect(jsonPath("$.[0].name").value(saved.getName()))
               .andExpect(jsonPath("$.[0].startDate").value(sameInstant(saved.getStartDate())))
               .andExpect(jsonPath("$.[0].updateDate").value(sameInstant(saved.getUpdateDate())))
               .andExpect(jsonPath("$.[0].endDate").value(sameInstant(saved.getEndDate())))
               .andExpect(jsonPath("$.[0].avatarUrl").value(containsString("aaaaa.jpg")))
               .andExpect(jsonPath("$.[0].description").value(saved.getDescription()))
               .andExpect(jsonPath("$.[0].data.AAAAAAAAAA").value("BBBBBBBBBB"))

               .andExpect(jsonPath("$.[0].tags[0].id").value(notNullValue()))
               .andExpect(jsonPath("$.[0].tags[0].name").value(tag.getName()))
               .andExpect(jsonPath("$.[0].tags[0].typeKey").value(tag.getTypeKey()))
               .andExpect(jsonPath("$.[0].tags[0].xmEntity").value(tag.getXmEntity().getId()))

               .andExpect(jsonPath("$.[0].attachments[0].id").value(notNullValue()))
               .andExpect(jsonPath("$.[0].attachments[0].typeKey").value(attachment.getTypeKey()))
               .andExpect(jsonPath("$.[0].attachments[0].name").value(attachment.getName()))
               .andExpect(jsonPath("$.[0].attachments[0].contentUrl").value(attachment.getContentUrl()))
               .andExpect(jsonPath("$.[0].attachments[0].description").value(attachment.getDescription()))
               .andExpect(jsonPath("$.[0].attachments[0].startDate").value(sameInstant(attachment.getStartDate())))
               .andExpect(jsonPath("$.[0].attachments[0].endDate").value(sameInstant(attachment.getEndDate())))
               .andExpect(jsonPath("$.[0].attachments[0].valueContentType").value(attachment.getValueContentType()))
               .andExpect(jsonPath("$.[0].attachments[0].valueContentSize").value(attachment.getValueContentSize()))
               .andExpect(jsonPath("$.[0].attachments[0].xmEntity").value(attachment.getXmEntity().getId()))

               .andExpect(jsonPath("$.[0].locations[0].id").value(notNullValue()))
               .andExpect(jsonPath("$.[0].locations[0].typeKey").value(location.getTypeKey()))
               .andExpect(jsonPath("$.[0].locations[0].name").value(location.getName()))
               .andExpect(jsonPath("$.[0].locations[0].countryKey").value(location.getCountryKey()))
               .andExpect(jsonPath("$.[0].locations[0].xmEntity").value(location.getXmEntity().getId()));
    }

    @SneakyThrows
    @Test
    @Transactional
    public void reindexWithDifferentTypeKeys() {
        Set<Long> saved  = Sets.newHashSet(
            xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY)).getId(),
            xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY)).getId(),
            xmEntityService.save(createEntityComplexIncoming().typeKey(ANOTHER_TYPE_KEY)).getId()
        );
        assertEquals(saved.size(), 3);
        long reindexed = elasticsearchIndexService.reindexAll();
        assertEquals(reindexed, 3);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+ANOTHER_TYPE_KEY))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$", hasSize(1)));
    }


    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByTypeKey() {

        autoIndexDisable();
        assertIndexIsEmpty(DEFAULT_TYPE_KEY);

        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY));

        long reindexed = elasticsearchIndexService.reindexByTypeKey(DEFAULT_TYPE_KEY);
        assertEquals(2L, reindexed);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
               .andExpect(jsonPath("$.[1].typeKey").value(DEFAULT_TYPE_KEY))
        ;
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByTypeKeyAsync() {

        autoIndexDisable();
        assertIndexIsEmpty(DEFAULT_TYPE_KEY);

        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY));

        long reindexed = elasticsearchIndexService.reindexByTypeKeyAsync(DEFAULT_TYPE_KEY).get();
        assertEquals(2L, reindexed);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[0].typeKey").value(DEFAULT_TYPE_KEY))
               .andExpect(jsonPath("$.[1].typeKey").value(DEFAULT_TYPE_KEY))
        ;
    }

    @Test
    @Transactional
    public void testReindexByTypeKeyNonNull() {

        autoIndexDisable();

        exception.expect(NullPointerException.class);
        exception.expectMessage("typeKey should not be null");

        String typeKey = null;
        elasticsearchIndexService.reindexByTypeKey(typeKey);

    }

    @Test
    @Transactional
    public void testReindexByTypeKeyAsyncNonNull() {

        autoIndexDisable();

        exception.expect(NullPointerException.class);
        exception.expectMessage("typeKey should not be null");

        String typeKey = null;
        elasticsearchIndexService.reindexByTypeKeyAsync(typeKey);

    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByIds() {

        autoIndexDisable();
        assertIndexIsEmpty(DEFAULT_TYPE_KEY);
        assertIndexIsEmpty(ANOTHER_TYPE_KEY);

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id1, id3));
        assertEquals(2L, reindexed);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(1)))
               .andExpect(jsonPath("$.[*].id")
                              .value(id1.intValue()))
        ;
        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+ANOTHER_TYPE_KEY))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[*].id")
                .value(id3.intValue()));
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByIdsAsync() {

        autoIndexDisable();
        assertIndexIsEmpty(DEFAULT_TYPE_KEY);

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexByIdsAsync(Lists.newArrayList(id1, id3)).get();
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(1)))
               .andExpect(jsonPath("$.[*].id").value(id1.intValue()))
        ;

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+ANOTHER_TYPE_KEY))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").value(hasSize(1)))
            .andExpect(jsonPath("$.[*].id").value(id3.intValue()))
        ;
    }

    @Test
    @Transactional
    public void testReindexByIdsNonNull() {

        autoIndexDisable();

        exception.expect(NullPointerException.class);
        exception.expectMessage("ids should not be null");

        List<Long> ids = null;
        elasticsearchIndexService.reindexByIds(ids);

    }

    @Test
    @Transactional
    public void testReindexByIdsAsyncNonNull() {

        autoIndexDisable();

        exception.expect(NullPointerException.class);
        exception.expectMessage("ids should not be null");

        List<Long> ids = null;
        elasticsearchIndexService.reindexByIds(ids);

    }


    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByIdsDoNotDeletesExistingIndex() {

        autoIndexDisable();
        assertIndexIsEmpty(DEFAULT_TYPE_KEY);
        assertIndexIsEmpty(ANOTHER_TYPE_KEY);

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id2 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id2 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id1, id2));
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(), id2.intValue())))
        ;

        reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id3));
        assertEquals(1L, reindexed);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(ANOTHER_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+ANOTHER_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(1)))
               .andExpect(jsonPath("$.[*].id").value(id3.intValue()))
        ;
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByIdsUpdatesIndexData() {

        autoIndexDisable();
        assertIndexIsEmpty(DEFAULT_TYPE_KEY);

        XmEntity xmEntity1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        XmEntity xmEntity2 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));

        assert xmEntity1.getId() != null;
        assert xmEntity2.getId() != null;

        List<Long> ids = Lists.newArrayList(xmEntity1.getId(), xmEntity2.getId());

        long reindexed = elasticsearchIndexService.reindexByIds(ids);
        assertEquals(2L, reindexed);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(xmEntity1.getId().intValue(),
                                                        xmEntity2.getId().intValue())))
               .andExpect(jsonPath("$.[*].name")
                              .value(containsInAnyOrder(DEFAULT_NAME, DEFAULT_NAME)))

        ;

        xmEntityService.save(xmEntity1.name("new name 1"));

        reindexed = elasticsearchIndexService.reindexByIds(ids);
        assertEquals(2L, reindexed);
        elasticsearchTemplate.refresh(elasticIndexNameResolver.resolve(DEFAULT_TYPE_KEY));

        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+DEFAULT_TYPE_KEY))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(xmEntity1.getId().intValue(),
                                                        xmEntity2.getId().intValue())))
               .andExpect(jsonPath("$.[*].name")
                              .value(containsInAnyOrder("new name 1", DEFAULT_NAME)))
        ;
    }

    private void assertIndexIsEmpty(String typeKey) throws Exception {
        mockMvc.perform(get("/api/_search-with-typekey/xm-entities?typeKey="+typeKey))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isEmpty())
        ;
    }

    private void reindexWithRestApi() throws Exception {
        mockMvc.perform(post("/api/elasticsearch/index"))
               .andExpect(status().isAccepted());
    }

    private void autoIndexDisable() {
        when(xmEntitySpecServiceMock.getTypeSpecByKey(anyString()))
            .thenReturn(createTypeSpecWith(false, false));
    }

    private Optional<TypeSpec> createTypeSpecWith(boolean indexAfterSave, boolean indexAfterDelete) {
        TypeSpec spec = new TypeSpec();
        spec.setIndexAfterSaveEnabled(indexAfterSave);
        spec.setIndexAfterDeleteEnabled(indexAfterDelete);
        return Optional.of(spec);
    }
}
