package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.AttachmentRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the AttachmentResource REST controller.
 *
 * @see AttachmentResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
@ActiveProfiles("avatar-file")
public class AttachmentResourceFsIntTest extends AbstractJupiterSpringBootTest {

    public static final String DEFAULT_TYPE_KEY = "AAAAAAAAFS";
    public static final String UPDATED_TYPE_KEY = "BBBBBBBBFS";

    public static final String DEFAULT_NAME = "AAAAAAAAFS";
    public static final String UPDATED_NAME = "BBBBBBBBFS";

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
    private PermittedRepository permittedRepository;

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
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private EntityManager em;

    private AttachmentService attachmentService;

    private MockMvc restAttachmentMockMvc;

    private XmEntity xmEntity;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private XmAuthenticationContextHolder authContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private ContentService contentService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() throws URISyntaxException {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(DEFAULT_START_DATE);

        attachmentService = new AttachmentService(attachmentRepository,
                                                  contentService,
                                                  permittedRepository,
                                                  startUpdateDateGenerationStrategy,
                                                  xmEntityRepository,
                                                  xmEntitySpecService);

        AttachmentResource attachmentResourceMock = new AttachmentResource(attachmentService, attachmentResource);
        this.restAttachmentMockMvc = MockMvcBuilders.standaloneSetup(attachmentResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();


        xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();

    }

    @BeforeEach
    public void initTest() {
        //  attachmentSearchRepository.deleteAll();
    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
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
    public void shouldCreateAndGetFileAttachment() throws Exception {
        int databaseSizeBeforeCreate = attachmentRepository.findAll().size();

        Attachment attachment = new Attachment();
        attachment.setTypeKey(DEFAULT_TYPE_KEY);
        attachment.setName(DEFAULT_NAME);
        attachment.setDescription(DEFAULT_DESCRIPTION);
        attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
        attachment.setContent(createContent("A"));

        XmEntity e = new XmEntity();
        e.setId(xmEntity.getId());

        attachment.setXmEntity(e);

        // Create the Attachment

        var result = restAttachmentMockMvc.perform(post("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(attachment)));

        result.andExpect(status().isCreated());

        // Validate the Attachment in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate + 1);
        Attachment testAttachment = attachmentList.getLast();
        assertThat(testAttachment.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testAttachment.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testAttachment.getContentUrl()).startsWith(FILE_PREFIX);
        assertThat(testAttachment.getContentUrl()).endsWith(DEFAULT_NAME);
        assertThat(testAttachment.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testAttachment.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testAttachment.getValueContentType()).isEqualTo(DEFAULT_VALUE_CONTENT_TYPE);
        assertThat(testAttachment.getValueContentSize()).isEqualTo(DEFAULT_VALUE_CONTENT_SIZE);

        restAttachmentMockMvc.perform(get("/api/attachments/{id}", testAttachment.getId()))
            .andExpect(status().is2xxSuccessful());

    }

    @Test
    @Transactional
    public void shouldCreateAndDeleteFileAttachment() throws Exception {
        int databaseSizeBeforeCreate = attachmentRepository.findAll().size();

        Attachment attachment = new Attachment();
        attachment.setTypeKey(DEFAULT_TYPE_KEY);
        attachment.setName(DEFAULT_NAME);
        attachment.setDescription(DEFAULT_DESCRIPTION);
        attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
        attachment.setContent(createContent("A"));

        XmEntity e = new XmEntity();
        e.setId(xmEntity.getId());

        attachment.setXmEntity(e);

        // Create the Attachment

        var result = restAttachmentMockMvc.perform(post("/api/attachments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(attachment)));

        result.andExpect(status().isCreated());

        // Validate the Attachment in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate + 1);

        result = restAttachmentMockMvc.perform(delete("/api/attachments/{id}", attachmentList.getLast().getId())
            .contentType(TestUtil.APPLICATION_JSON_UTF8));

        result.andExpect(status().is2xxSuccessful());

        attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate);

    }


    @Test
    @Transactional
    public void shouldCreateAndUpdateFileAttachment() throws Exception {
        int databaseSizeBeforeCreate = attachmentRepository.findAll().size();

        Attachment attachment = new Attachment();
        attachment.setTypeKey(DEFAULT_TYPE_KEY);
        attachment.setName(DEFAULT_NAME);
        attachment.setDescription(DEFAULT_DESCRIPTION);
        attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
        attachment.setContent(createContent("A"));

        XmEntity e = new XmEntity();
        e.setId(xmEntity.getId());

        attachment.setXmEntity(e);

        // Create the Attachment

        var result = restAttachmentMockMvc.perform(post("/api/attachments")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(attachment)));

        result.andExpect(status().isCreated());

        // Validate the Attachment in the database
        List<Attachment> attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate + 1);

        //By this time file is created
        result = restAttachmentMockMvc.perform(get("/api/attachments/{id}", attachmentList.getLast().getId())
            .contentType(TestUtil.APPLICATION_JSON_UTF8));

        result.andExpect(status().is2xxSuccessful());

        // Update the attachment
        Attachment updatedAttachment = attachmentList.getLast();

        updatedAttachment
            .typeKey(UPDATED_TYPE_KEY)
            .name(UPDATED_NAME)
             //.contentUrl(UPDATED_CONTENT_URL)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .endDate(UPDATED_END_DATE)
            .valueContentType(UPDATED_VALUE_CONTENT_TYPE)
            .valueContentSize(UPDATED_VALUE_CONTENT_SIZE)
            .content(createContent("AA"));

        restAttachmentMockMvc.perform(put("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedAttachment)))
            .andExpect(status().isOk());

        // Validate the Attachment in the database
        attachmentList = attachmentRepository.findAll();
        assertThat(attachmentList).hasSize(databaseSizeBeforeCreate + 1);

        //By this time file is created
        result = restAttachmentMockMvc.perform(get("/api/attachments/{id}", attachmentList.getLast().getId())
            .contentType(TestUtil.APPLICATION_JSON_UTF8));

        result.andExpect(status().is2xxSuccessful());

    }


    private static Content createContent(String value) {
        Content content = new Content();
        content.setValue(value.getBytes());
        return content;
    }

}
