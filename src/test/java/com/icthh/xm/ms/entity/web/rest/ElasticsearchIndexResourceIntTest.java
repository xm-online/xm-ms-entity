package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.web.rest.TestUtil.sameInstant;
import static com.icthh.xm.ms.entity.web.rest.XmEntityResourceExtendedIntTest.createEntityComplexIncoming;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.elasticsearch.EmbeddedElasticsearchConfig;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import java.util.concurrent.Executor;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Executor;

/**
 * Test class for the ElasticsearchIndexResource REST controller.
 *
 * @see ElasticsearchIndexResource
 */
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    EmbeddedElasticsearchConfig.class
})
public class ElasticsearchIndexResourceIntTest {

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";

    private MockMvc mockMvc;

    @Autowired
    private XmEntityService xmEntityService;

    @Autowired
    private ElasticsearchIndexResource elasticsearchIndexResource;

    @Autowired
    private ElasticsearchIndexService elasticsearchIndexService;

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

    @MockBean(name = "taskExecutor")
    private Executor executor;

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

        this.mockMvc = MockMvcBuilders.standaloneSetup(elasticsearchIndexResource, xmEntityResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
        doAnswer(a -> {
            ((Runnable)a.getArguments()[0]).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @After
    @Override
    public void finalize() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @SneakyThrows
    @Test
    @Transactional
    public void reindex() {
        mockMvc.perform(post("/api/elasticsearch/index"))
            .andExpect(status().isAccepted());
    }

    @SneakyThrows
    @Test
    @Transactional
    public void reindexComplexEntity() {
        XmEntity saved = xmEntityService.save(createEntityComplexIncoming().typeKey(DEFAULT_TYPE_KEY));
        Tag tag = saved.getTags().iterator().next();
        Attachment attachment = saved.getAttachments().iterator().next();
        Location location = saved.getLocations().iterator().next();

        mockMvc.perform(post("/api/elasticsearch/index"))
            .andExpect(status().isAccepted());

        searchRepository.refresh();

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
}
