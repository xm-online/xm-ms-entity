package com.icthh.xm.ms.entity.service;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
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
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ee.commons.search.ElasticsearchTemplateWrapper;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractElasticSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
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
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.web.rest.ElasticsearchIndexResource;
import com.icthh.xm.ms.entity.web.rest.XmEntityResource;

import jakarta.persistence.EntityManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Test class for the ElasticsearchIndexResource REST controller and ElasticsearchIndexService service.
 *
 * @see ElasticsearchIndexResource
 * @see ElasticsearchIndexService
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class ElasticsearchIndexResourceIntTest extends AbstractElasticSpringBootTest {

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String ANOTHER_TYPE_KEY = "ACCOUNT.OWNER";
    private static final String NO_INDEX_TYPE_KEY = "ACCOUNT.NO_ELASTIC_SAVE";
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
    private ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

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
    private PermittedSearchRepository permittedSearchRepository;

    @Autowired
    private SeparateTransactionExecutor transactionExecutor;

    @Autowired
    private PermissionCheckService permissionCheckService;

    @Autowired
    private ApplicationProperties applicationProperties;

    private ElasticsearchIndexService elasticsearchIndexService;

    @Mock
    private Executor executor;

    @Mock
    private XmEntitySpecService xmEntitySpecServiceMock;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() throws IOException {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authenticationContextHolder.getContext());
        });

        initElasticsearch(tenantContextHolder);

        // ???
        xmEntityRepositoryInternal.deleteAll();
        cleanElasticsearch(tenantContextHolder);

        mappingConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/mapping.json", null);
        indexConfiguration.onRefresh("/config/tenants/RESINTTEST/entity/index_config.json", null);

        elasticsearchIndexService = new ElasticsearchIndexService(xmEntityRepositoryInternal,
                                                                  xmEntitySearchRepository,
                                                                  elasticsearchTemplateWrapper,
                                                                  tenantContextHolder,
                                                                  mappingConfiguration,
                                                                  indexConfiguration,
                                                                  executor, entityManager,
                                                                  applicationProperties,
                                                                  xmEntitySpecService);

        elasticsearchTemplateWrapper.refresh(XmEntity.class);

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

    @AfterEach
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
        log.info("Current repository state {}", ((Page)searchRepository.findAll()).getContent());

        XmEntity saved = xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY));
        Tag tag = saved.getTags().iterator().next();
        Attachment attachment = saved.getAttachments().iterator().next();
        Location location = saved.getLocations().iterator().next();

//        saved = xmEntityService.findOne(IdOrKey.of(saved.getId()));

        reindexWithRestApi();

        searchRepository.refresh();

        log.info("Current repository state {}", ((Page)searchRepository.findAll()).getContent());
        log.info("Current permitted repository state {}", permittedSearchRepository.search("id:" + saved.getId(),
                PageRequest.of(0, 20), XmEntity.class, "XMENTITY.SEARCH").getContent());

        assert attachment.getXmEntity().getId() != null;
        assert location.getXmEntity().getId() != null;
        assert saved.getId() != null;
        assert tag.getXmEntity().getId() != null;

        mockMvc.perform(get("/api/_search/xm-entities?query=id:{id}", saved.getId()))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(3)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(), id2.intValue(), id3.intValue())))
        ;

        xmEntityRepositoryInternal.deleteAll();

        Long id5 = xmEntityService.save(createEntity().typeKey(DEFAULT_TYPE_KEY)).getId();
        Long id6 = xmEntityService.save(createEntity().typeKey(ANOTHER_TYPE_KEY)).getId();
        Long id7 = xmEntityService.save(createEntity().typeKey(NO_INDEX_TYPE_KEY)).getId();

        assert id5 != null;
        assert id6 != null;
        assert id7 != null;

        reindexed = elasticsearchIndexService.reindexAll();
        assertEquals(2L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id").value(containsInAnyOrder(id5.intValue(), id6.intValue())))
        ;
    }


    @Test
    @SneakyThrows
    @Transactional
    public void testReindexNoneAfterReindex() {

        autoIndexDisable();
        assertIndexIsEmpty();

        Long id1 = xmEntityService.save(createEntity().typeKey(NO_INDEX_TYPE_KEY)).getId();
        Long id2 = xmEntityService.save(createEntity().typeKey(NO_INDEX_TYPE_KEY)).getId();

        assert id1 != null;
        assert id2 != null;

        long reindexed = elasticsearchIndexService.reindexAll();
        assertEquals(0L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").value(hasSize(0)))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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

        String typeKey = null;
        assertThrows("typeKey should not be null", NullPointerException.class, () -> {
            elasticsearchIndexService.reindexByTypeKey(typeKey);
        });
    }

    @Test
    @Transactional
    public void testReindexByTypeKeyAsyncNonNull() {

        autoIndexDisable();

        String typeKey = null;

        assertThrows("typeKey should not be null", NullPointerException.class, () -> {
            elasticsearchIndexService.reindexByTypeKeyAsync(typeKey);
        });

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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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

        List<Long> ids = null;

        assertThrows("ids should not be null", NullPointerException.class, () -> {
            elasticsearchIndexService.reindexByIds(ids);
        });


    }

    @Test
    @Transactional
    public void testReindexByIdsAsyncNonNull() {

        autoIndexDisable();

        List<Long> ids = null;
        assertThrows("ids should not be null", NullPointerException.class, () -> {
            elasticsearchIndexService.reindexByIds(ids);
        });
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$").isArray())
               .andExpect(jsonPath("$").value(hasSize(2)))
               .andExpect(jsonPath("$.[*].id")
                              .value(containsInAnyOrder(id1.intValue(), id2.intValue())))
        ;

        reindexed = elasticsearchIndexService.reindexByIds(Lists.newArrayList(id3));
        assertEquals(1L, reindexed);

        mockMvc.perform(get("/api/_search/xm-entities?query=*:*"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
