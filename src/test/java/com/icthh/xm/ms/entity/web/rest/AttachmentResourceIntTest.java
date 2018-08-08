package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.AttachmentSearchRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * Test class for the AttachmentResource REST controller.
 *
 * @see AttachmentResource
 */
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class AttachmentResourceIntTest {

    public static final String DEFAULT_TYPE_KEY = "AAAAAAAAAA";
    public static final String UPDATED_TYPE_KEY = "BBBBBBBBBB";

    public static final String DEFAULT_NAME = "AAAAAAAAAA";
    public static final String UPDATED_NAME = "BBBBBBBBBB";

    public static final String DEFAULT_CONTENT_URL = "AAAAAAAAAA";
    public static final String UPDATED_CONTENT_URL = "BBBBBBBBBB";

    public static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    public static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    public static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    public static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    public static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    public static final String DEFAULT_VALUE_CONTENT_TYPE = "image/jpg";
    public static final String UPDATED_VALUE_CONTENT_TYPE = "image/png";

    public static final Long DEFAULT_VALUE_CONTENT_SIZE = 1L;
    public static final Long UPDATED_VALUE_CONTENT_SIZE = 2L;

    @Autowired
    private AttachmentResource attachmentResource;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private AttachmentSearchRepository attachmentSearchRepository;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private PermittedSearchRepository permittedSearchRepository;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private EntityManager em;

    private AttachmentService attachmentService;

    private MockMvc restAttachmentMockMvc;

    private Attachment attachment;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() throws URISyntaxException {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(DEFAULT_START_DATE);

        attachmentService = new AttachmentService(attachmentRepository,
                                                  attachmentSearchRepository,
                                                  permittedRepository,
                                                  permittedSearchRepository,
                                                  startUpdateDateGenerationStrategy,
                                                  xmEntityRepository);

        AttachmentResource attachmentResourceMock = new AttachmentResource(attachmentService, attachmentResource);
        this.restAttachmentMockMvc = MockMvcBuilders.standaloneSetup(attachmentResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();

        attachment = createEntity(em);
    }

    @Before
    public void initTest() {
        //  attachmentSearchRepository.deleteAll();
    }

    @After
    @Override
    public void finalize() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Attachment createEntity(EntityManager em) {
        Attachment attachment = new Attachment()
            .typeKey(DEFAULT_TYPE_KEY)
            .name(DEFAULT_NAME)
            .contentUrl(DEFAULT_CONTENT_URL)
            .description(DEFAULT_DESCRIPTION)
            .startDate(DEFAULT_START_DATE)
            .endDate(DEFAULT_END_DATE)
            .valueContentType(DEFAULT_VALUE_CONTENT_TYPE)
            .valueContentSize(DEFAULT_VALUE_CONTENT_SIZE);
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();
        attachment.setXmEntity(xmEntity);
        return attachment;
    }

    @Test
    @Transactional
    public void createAttachment() throws Exception {
        int databaseSizeBeforeCreate = attachmentRepository.findAll().size();

        // Create the Attachment
        restAttachmentMockMvc.perform(post("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isCreated());

        // Validate the Attachment in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate + 1);
        Attachment testAttachment = attachmentList.get(attachmentList.size() - 1);
        assertThat(testAttachment.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testAttachment.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testAttachment.getContentUrl()).isEqualTo(DEFAULT_CONTENT_URL);
        assertThat(testAttachment.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testAttachment.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testAttachment.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testAttachment.getValueContentType()).isEqualTo(DEFAULT_VALUE_CONTENT_TYPE);
        assertThat(testAttachment.getValueContentSize()).isEqualTo(DEFAULT_VALUE_CONTENT_SIZE);

        // Validate the Attachment in Elasticsearch
        Attachment attachmentEs = attachmentSearchRepository.findOne(testAttachment.getId());
        assertThat(attachmentEs).isEqualToComparingFieldByField(testAttachment);
    }

    @Test
    @Transactional
    public void createAttachmentWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = attachmentRepository.findAll().size();

        // Create the Attachment with an existing ID
        attachment.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restAttachmentMockMvc.perform(post("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = attachmentRepository.findAll().size();
        // set the field null
        attachment.setTypeKey(null);

        // Create the Attachment, which fails.

        restAttachmentMockMvc.perform(post("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("attachment"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("typeKey"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = attachmentRepository.findAll().size();
        // set the field null
        attachment.setName(null);

        // Create the Attachment, which fails.

        restAttachmentMockMvc.perform(post("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("attachment"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Ignore("Content url not required temporary")
    public void checkContentUrlIsRequired() throws Exception {
        int databaseSizeBeforeTest = attachmentRepository.findAll().size();
        // set the field null
        attachment.setContentUrl(null);

        // Create the Attachment, which fails.

        restAttachmentMockMvc.perform(post("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("attachment"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("contentUrl"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Ignore("see AttachmentResourceExtendedIntTest.checkStartDateIsNotRequired instead")
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = attachmentRepository.findAll().size();
        // set the field null
        attachment.setStartDate(null);

        // Create the Attachment, which fails.

        restAttachmentMockMvc.perform(post("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("attachment"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllAttachments() throws Exception {
        // Initialize the database
        attachmentRepository.saveAndFlush(attachment);

        // Get all the attachmentList
        restAttachmentMockMvc.perform(get("/api/attachments?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(attachment.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].contentUrl").value(hasItem(DEFAULT_CONTENT_URL.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].valueContentType").value(hasItem(DEFAULT_VALUE_CONTENT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].valueContentSize").value(hasItem(DEFAULT_VALUE_CONTENT_SIZE.intValue())));
    }

    @Test
    @Transactional
    public void getAttachment() throws Exception {
        // Initialize the database
        attachmentRepository.saveAndFlush(attachment);

        // Get the attachment
        restAttachmentMockMvc.perform(get("/api/attachments/{id}", attachment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(attachment.getId().intValue()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.contentUrl").value(DEFAULT_CONTENT_URL.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.valueContentType").value(DEFAULT_VALUE_CONTENT_TYPE.toString()))
            .andExpect(jsonPath("$.valueContentSize").value(DEFAULT_VALUE_CONTENT_SIZE.intValue()));
    }

    @Test
    @Transactional
    public void getNonExistingAttachment() throws Exception {
        // Get the attachment
        restAttachmentMockMvc.perform(get("/api/attachments/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateAttachment() throws Exception {
        // Initialize the database
        attachmentService.save(attachment);

        int databaseSizeBeforeUpdate = attachmentRepository.findAll().size();

        // Update the attachment
        Attachment updatedAttachment = attachmentRepository.findOne(attachment.getId());
        updatedAttachment
            .typeKey(UPDATED_TYPE_KEY)
            .name(UPDATED_NAME)
            .contentUrl(UPDATED_CONTENT_URL)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .valueContentType(UPDATED_VALUE_CONTENT_TYPE)
            .valueContentSize(UPDATED_VALUE_CONTENT_SIZE);

        restAttachmentMockMvc.perform(put("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(updatedAttachment)))
            .andExpect(status().isOk());

        // Validate the Attachment in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeUpdate);
        Attachment testAttachment = attachmentList.get(attachmentList.size() - 1);
        assertThat(testAttachment.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testAttachment.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testAttachment.getContentUrl()).isEqualTo(UPDATED_CONTENT_URL);
        assertThat(testAttachment.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testAttachment.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testAttachment.getEndDate()).isEqualTo(UPDATED_END_DATE);
        assertThat(testAttachment.getValueContentType()).isEqualTo(UPDATED_VALUE_CONTENT_TYPE);
        assertThat(testAttachment.getValueContentSize()).isEqualTo(UPDATED_VALUE_CONTENT_SIZE);

        // Validate the Attachment in Elasticsearch
        Attachment attachmentEs = attachmentSearchRepository.findOne(testAttachment.getId());
        assertThat(attachmentEs).isEqualToComparingFieldByField(testAttachment);
    }

    @Test
    @Transactional
    public void updateNonExistingAttachment() throws Exception {
        int databaseSizeBeforeUpdate = attachmentRepository.findAll().size();

        // Create the Attachment

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restAttachmentMockMvc.perform(put("/api/attachments")
                                          .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                          .content(TestUtil.convertObjectToJsonBytes(attachment)))
            .andExpect(status().isCreated());

        // Validate the Attachment in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteAttachment() throws Exception {
        // Initialize the database
        attachmentService.save(attachment);

        int databaseSizeBeforeDelete = attachmentRepository.findAll().size();

        // Get the attachment
        restAttachmentMockMvc.perform(delete("/api/attachments/{id}", attachment.getId())
                                          .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate Elasticsearch is empty
        boolean attachmentExistsInEs = attachmentSearchRepository.exists(attachment.getId());
        assertThat(attachmentExistsInEs).isFalse();

        // Validate the database is empty
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchAttachment() throws Exception {
        // Initialize the database
        attachmentService.save(attachment);

        // Search the attachment
        restAttachmentMockMvc.perform(get("/api/_search/attachments?query=id:" + attachment.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(attachment.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].contentUrl").value(hasItem(DEFAULT_CONTENT_URL.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].valueContentType").value(hasItem(DEFAULT_VALUE_CONTENT_TYPE.toString())))
            .andExpect(jsonPath("$.[*].valueContentSize").value(hasItem(DEFAULT_VALUE_CONTENT_SIZE.intValue())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Attachment.class);
        Attachment attachment1 = new Attachment();
        attachment1.setId(1L);
        Attachment attachment2 = new Attachment();
        attachment2.setId(attachment1.getId());
        assertThat(attachment1).isEqualTo(attachment2);
        attachment2.setId(2L);
        assertThat(attachment1).isNotEqualTo(attachment2);
        attachment1.setId(null);
        assertThat(attachment1).isNotEqualTo(attachment2);
    }
}
