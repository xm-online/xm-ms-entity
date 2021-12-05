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
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.IndexConfiguration;
import com.icthh.xm.ms.entity.config.MappingConfiguration;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.listener.XmEntityElasticSearchListener;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.web.rest.ElasticsearchIndexResource;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;
import lombok.SneakyThrows;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Test class for the ElasticsearchIndexResource REST controller and ElasticsearchIndexService service.
 *
 * @see ElasticsearchIndexResource
 * @see ElasticsearchIndexService
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class ElasticsearchIndexResourceIntTest extends AbstractSpringBootTest {

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String ANOTHER_TYPE_KEY = "ACCOUNT.OWNER";
    private static final String DEFAULT_NAME = "AAAAAAAAAA";

    private static boolean elasticInited = false;

    private MockMvc mockMvc;

    @Autowired
    private XmEntityService xmEntityService;

    @Autowired
    private XmEntityResource xmEntityResource;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authenticationContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntitySearchRepository searchRepository;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepositoryInternal;

    @Autowired
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private MappingConfiguration mappingConfiguration;

    @Autowired
    private IndexConfiguration indexConfiguration;

    @Autowired
    private XmEntityElasticSearchListener xmEntityElasticSearchListener;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SeparateTransactionExecutor transactionExecutor;

    private ElasticsearchIndexService elasticsearchIndexService;

    @Mock
    private Executor executor;

    @Mock
    private XmEntitySpecService xmEntitySpecServiceMock;

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
            initElasticsearch();
            elasticInited = true;
        }
        // ???
        xmEntityRepositoryInternal.deleteAll();
        cleanElasticsearch();
        elasticsearchTemplate.refresh(XmEntity.class);

        elasticsearchIndexService = new ElasticsearchIndexService(xmEntityRepositoryInternal,
                                                                  xmEntitySearchRepository,
                                                                  elasticsearchTemplate,
                                                                  tenantContextHolder,
                                                                  mappingConfiguration,
                                                                  indexConfiguration,
                                                                  executor, entityManager);

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
        XmEntity saved = transactionExecutor.doInSeparateTransaction(() -> {
            return xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY));
        });
        Tag tag = saved.getTags().iterator().next();
        Attachment attachment = saved.getAttachments().iterator().next();
        Location location = saved.getLocations().iterator().next();

//        saved = xmEntityService.findOne(IdOrKey.of(saved.getId()));

        reindexWithRestApi();

        searchRepository.refresh();

        assert attachment.getXmEntity().getId() != null;
        assert location.getXmEntity().getId() != null;
        assert saved.getId() != null;
        assert tag.getXmEntity().getId() != null;

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", saved.getId()))
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
    public void autoIndexAfterSaveDisabled() {

        Long id = transactionExecutor.doInSeparateTransaction(() -> {
            when(xmEntitySpecServiceMock.getTypeSpecByKeyWithoutFunctionFilter(DEFAULT_TYPE_KEY))
                .thenReturn(createTypeSpecWith(false));
            return createAndFlush();
        });

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", id))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isEmpty())
        ;
    }

    @SneakyThrows
    @Test
    @Transactional
    public void autoIndexAfterSaveEnabled() {

        Long id = transactionExecutor.doInSeparateTransaction(() -> {
            when(xmEntitySpecServiceMock.getTypeSpecByKeyWithoutFunctionFilter(DEFAULT_TYPE_KEY))
                .thenReturn(createTypeSpecWith(true));
            return createAndFlush();
        });

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", id))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isNotEmpty())
               .andExpect(jsonPath("$.[0].id").value(id))
        ;
    }

    @SneakyThrows
    @Test
    @Transactional
    public void autoIndexAfterDeleteDisabled() {

        Long id = transactionExecutor.doInSeparateTransaction(() -> {
            when(xmEntitySpecServiceMock.getTypeSpecByKeyWithoutFunctionFilter(DEFAULT_TYPE_KEY))
                .thenReturn(createTypeSpecWith(true, false));
            return createAndFlush();
        });

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", id))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isNotEmpty())
               .andExpect(jsonPath("$.[0].id").value(id))
        ;

        transactionExecutor.doInSeparateTransaction(() -> {
            xmEntityService.delete(id);
            assertFalse(xmEntityRepositoryInternal.existsById(id));
            return null;
        });

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", id))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isNotEmpty())
               .andExpect(jsonPath("$.[0].id").value(id))
        ;
    }

    @SneakyThrows
    @Test
    @Transactional
    public void autoIndexAfterDeleteEnabled() {

        Long id = transactionExecutor.doInSeparateTransaction(() -> {
            when(xmEntitySpecServiceMock.getTypeSpecByKeyWithoutFunctionFilter(DEFAULT_TYPE_KEY))
                .thenReturn(createTypeSpecWith(true, true));
            return createAndFlush();
        });

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", id))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isNotEmpty())
               .andExpect(jsonPath("$.[0].id").value(id))
        ;

        transactionExecutor.doInSeparateTransaction(() -> {
            xmEntityService.delete(id);
            assertFalse(xmEntityRepositoryInternal.existsById(id));
            return null;
        });

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", id))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").isEmpty())
        ;
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexAll() {

        autoIndexDisable();
        assertIndexIsEmpty();

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id2 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id2 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexAll();
        assertEquals(3L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(3)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(), id2.intValue(), id3.intValue())))
        ;

        xmEntityRepositoryInternal.deleteAll();

        Long id4 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id5 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();
        assert id4 != null;
        assert id5 != null;

        reindexed = elasticsearchIndexService.reindexAll();
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id").value(containsInAnyOrder(id4.intValue(), id5.intValue())))
        ;
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByTypeKey() {

        autoIndexDisable();
        assertIndexIsEmpty();

        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY));

        long reindexed = elasticsearchIndexService.reindexByTypeKey(DEFAULT_TYPE_KEY);
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
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
        assertIndexIsEmpty();

        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY));

        long reindexed = elasticsearchIndexService.reindexByTypeKeyAsync(DEFAULT_TYPE_KEY).get();
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
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
        assertIndexIsEmpty();

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id1, id3));
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(),id3.intValue())))
        ;
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByIdsAsync() {

        autoIndexDisable();
        assertIndexIsEmpty();

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexByIdsAsync(Lists.newArrayList(id1, id3)).get();
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(),id3.intValue())))
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
        assertIndexIsEmpty();

        Long id1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id2 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id3 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();

        assert id1 != null;
        assert id2 != null;
        assert id3 != null;

        long reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id1, id2));
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(), id2.intValue())))
        ;

        reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id3));
        assertEquals(1L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(3)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(), id2.intValue(), id3.intValue())))
        ;
    }

    @Test
    @SneakyThrows
    @Transactional
    public void testReindexByIdsUpdatesIndexData() {

        autoIndexDisable();
        assertIndexIsEmpty();

        XmEntity xmEntity1 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));
        XmEntity xmEntity2 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY));

        assert xmEntity1.getId() != null;
        assert xmEntity2.getId() != null;

        List<Long> ids = Lists.newArrayList(xmEntity1.getId(), xmEntity2.getId());

        long reindexed = elasticsearchIndexService.reindexByIds(ids);
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
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

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
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

    private void assertIndexIsEmpty() throws Exception {
        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
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
        when(xmEntitySpecServiceMock.getTypeSpecByKeyWithoutFunctionFilter(anyString()))
            .thenReturn(createTypeSpecWith(false, false));
    }

    private Long createAndFlush() {
        Long id = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        xmEntityService.findOne(IdOrKey.of(id));
        return id;
    }

    private Optional<TypeSpec> createTypeSpecWith(boolean indexAfterSave) {
        return createTypeSpecWith(indexAfterSave, true);
    }

    private Optional<TypeSpec> createTypeSpecWith(boolean indexAfterSave, boolean indexAfterDelete) {
        TypeSpec spec = new TypeSpec();
        spec.setIndexAfterSaveEnabled(indexAfterSave);
        spec.setIndexAfterDeleteEnabled(indexAfterDelete);
        return Optional.of(spec);
    }
}
