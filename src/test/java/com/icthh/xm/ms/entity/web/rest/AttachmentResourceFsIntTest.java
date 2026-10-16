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
import com.icthh.xm.ms.entity.service.dto.AttachmentDto;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.service.ContentService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.validator.AttachmentContentTypeValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.icthh.xm.ms.entity.web.rest.facade.AttachmentFacade;
import com.icthh.xm.ms.entity.service.mapper.AttachmentMapper;

/**
 * Test class for the AttachmentResource REST controller.
 *
 * @see AttachmentResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
@ActiveProfiles("avatar-file")
public class AttachmentResourceFsIntTest extends AbstractJupiterSpringBootTest {

    public static final String DEFAULT_TYPE_KEY = "AAAAAAAAFS";
    public static final String DB_TYPE_KEY = "AAAAAAAAAA";
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
    private JsonMapper jsonMapper;

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

    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private AttachmentContentTypeValidator attachmentContentTypeValidator;

    @Autowired
    private ApplicationProperties applicationProperties;

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
                                                  xmEntitySpecService, attachmentContentTypeValidator, applicationProperties);

        attachmentService.setSelf(attachmentService);
        AttachmentFacade attachmentFacade = new AttachmentFacade(attachmentService, attachmentMapper);
        AttachmentResource attachmentResourceMock = new AttachmentResource(attachmentFacade, attachmentResource);
        this.restAttachmentMockMvc = MockMvcBuilders.standaloneSetup(attachmentResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper)).build();


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
                .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(attachment, attachmentMapper.toDto(attachment))));

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
            .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(attachment, attachmentMapper.toDto(attachment))));

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
            .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(attachment, attachmentMapper.toDto(attachment))));

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

    @Test
    @Transactional
    public void shouldDeleteFileOfFsAttachment() throws Exception {
        Attachment attachment = new Attachment();
        attachment.setTypeKey(DEFAULT_TYPE_KEY);
        attachment.setName(DEFAULT_NAME);
        attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
        attachment.setContent(createContent("A"));

        XmEntity e = new XmEntity();
        e.setId(xmEntity.getId());
        attachment.setXmEntity(e);

        restAttachmentMockMvc.perform(post("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(attachment, attachmentMapper.toDto(attachment))))
            .andExpect(status().isCreated());

        Attachment saved = attachmentRepository.findAll().getLast();
        Path file = tenantRoot().resolve(StringUtils.substringAfter(saved.getContentUrl(), FILE_PREFIX));
        assertThat(file).exists();

        restAttachmentMockMvc.perform(delete("/api/attachments/{id}", saved.getId()))
            .andExpect(status().is2xxSuccessful());

        assertThat(file).doesNotExist();
    }

    @Test
    @Transactional
    public void shouldNotDeleteOutsideTenantFolderByContentUrlOfDbAttachment() throws Exception {
        Path probe = createProbe();
        try {
            // DB store type in the spec, the content url points to a folder next to the tenant folder
            Attachment attachment = attachmentRepository.saveAndFlush(
                attachmentWithContentUrl("AAAAAAAAAA", FILE_PREFIX + "../" + probe.getFileName()));

            restAttachmentMockMvc.perform(delete("/api/attachments/{id}", attachment.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("error.file.path"));

            assertThat(probe.resolve("sentinel.txt")).exists();
        } finally {
            FileUtils.deleteQuietly(probe.toFile());
        }
    }

    @Test
    @Transactional
    public void shouldRejectContentUrlOutsideTenantFolder() throws Exception {
        Path probe = createProbe();
        try {
            // the service path (used by LEP) still checks the tenant folder
            Attachment attachment = attachmentWithContentUrl(DEFAULT_TYPE_KEY,
                FILE_PREFIX + "../" + probe.getFileName() + "/sentinel.txt");

            assertThatThrownBy(() -> attachmentService.save(attachment))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "error.file.path");
        } finally {
            FileUtils.deleteQuietly(probe.toFile());
        }
    }

    @Test
    @Transactional
    public void shouldRejectStorageContentUrlOnCreate() throws Exception {
        Attachment stored = createFsAttachment("A");

        // an FS attachment is created by a client only with the content, the content url is set by the storage
        for (String contentUrl : List.of(stored.getContentUrl(), FILE_PREFIX + entityFolder(stored), "bucket::key", "report.pdf")) {
            Attachment attachment = attachmentWithContentUrl(DEFAULT_TYPE_KEY, contentUrl);
            restAttachmentMockMvc.perform(post("/api/attachments")
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(TestUtil.convertObjectToJsonBytes(attachmentMapper.toDto(attachment))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("error.content.url.not.allowed"));
        }
    }

    @Test
    @Transactional
    public void shouldAcceptStorageContentUrlOnCreateWhenContentUrlCheckIsOff() throws Exception {
        Attachment stored = createFsAttachment("A");
        applicationProperties.setSecureAttachmentContentUrl(false);
        try {
            Attachment attachment = attachmentWithContentUrl(DEFAULT_TYPE_KEY, stored.getContentUrl());
            restAttachmentMockMvc.perform(post("/api/attachments")
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(TestUtil.convertObjectToJsonBytes(attachmentMapper.toDto(attachment))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentUrl").value(stored.getContentUrl()));
        } finally {
            applicationProperties.setSecureAttachmentContentUrl(true);
        }
    }

    @Test
    @Transactional
    public void shouldKeepStoredContentUrlOnUpdate() throws Exception {
        Attachment stored = createFsAttachment("A");
        String contentUrl = stored.getContentUrl();

        AttachmentDto dto = attachmentMapper.toDto(stored);
        dto.setContent(null);
        dto.setDescription(UPDATED_DESCRIPTION);

        restAttachmentMockMvc.perform(put("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentUrl").value(contentUrl))
            .andExpect(jsonPath("$.description").value(UPDATED_DESCRIPTION));
    }

    @Test
    @Transactional
    public void shouldKeepStoredContentUrlOnUpdateWithChangedContentUrl() throws Exception {
        Attachment victim = createFsAttachment("A");
        Path victimFile = tenantRoot().resolve(StringUtils.substringAfter(victim.getContentUrl(), FILE_PREFIX));
        Attachment own = createFsAttachment("B");
        String ownUrl = own.getContentUrl();

        for (String contentUrl : Arrays.asList(victim.getContentUrl(), FILE_PREFIX + entityFolder(victim), "bucket::key", null)) {
            AttachmentDto dto = attachmentMapper.toDto(own);
            dto.setContent(null);
            dto.setContentUrl(contentUrl);

            restAttachmentMockMvc.perform(put("/api/attachments")
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(TestUtil.convertObjectToJsonBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentUrl").value(ownUrl));
        }
        assertThat(attachmentRepository.findById(own.getId()).orElseThrow().getContentUrl()).isEqualTo(ownUrl);

        restAttachmentMockMvc.perform(delete("/api/attachments/{id}", own.getId()))
            .andExpect(status().is2xxSuccessful());
        assertThat(victimFile).hasContent("A");
    }

    @Test
    @Transactional
    public void shouldIgnoreClientContentUrlOnCreateWithContent() throws Exception {
        Attachment victim = createFsAttachment("A");
        Path victimFile = tenantRoot().resolve(StringUtils.substringAfter(victim.getContentUrl(), FILE_PREFIX));

        // the webapp sends the file name as the content url, a storage url of another file is ignored as well
        for (String contentUrl : List.of("report.pdf", victim.getContentUrl())) {
            Attachment attachment = attachmentWithContentUrl(DEFAULT_TYPE_KEY, contentUrl);
            attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
            attachment.setContent(createContent("B"));

            restAttachmentMockMvc.perform(post("/api/attachments")
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(TestUtil.convertObjectToJsonBytes(attachmentMapper.toDto(attachment))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentUrl").value(startsWith(FILE_PREFIX)))
                .andExpect(jsonPath("$.contentUrl").value(not(victim.getContentUrl())));
        }
        assertThat(victimFile).hasContent("A");
    }

    @Test
    @Transactional
    public void shouldRejectStorageContentUrlOfDbAttachment() throws Exception {
        Attachment stored = createFsAttachment("A");

        for (String contentUrl : List.of(stored.getContentUrl(), "bucket::key")) {
            for (Content content : Arrays.asList(createContent("B"), null)) {
                Attachment attachment = attachmentWithContentUrl(DB_TYPE_KEY, contentUrl);
                attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
                attachment.setContent(content);

                restAttachmentMockMvc.perform(post("/api/attachments")
                        .contentType(TestUtil.APPLICATION_JSON_UTF8)
                        .content(TestUtil.convertObjectToJsonBytes(attachmentMapper.toDto(attachment))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("error.content.url.not.allowed"));
            }
        }
    }

    @Test
    @Transactional
    public void shouldAcceptDbContentUrlOfDbAttachment() throws Exception {
        for (String contentUrl : List.of("report.pdf", "https://example.com/doc.pdf")) {
            Attachment attachment = attachmentWithContentUrl(DB_TYPE_KEY, contentUrl);
            attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
            attachment.setContent(createContent("B"));

            restAttachmentMockMvc.perform(post("/api/attachments")
                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                    .content(TestUtil.convertObjectToJsonBytes(attachmentMapper.toDto(attachment))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentUrl").value(contentUrl));
        }
    }

    @Test
    @Transactional
    public void shouldKeepStoredStorageContentUrlOfDbAttachmentOnUpdate() throws Exception {
        // the store type of the spec changed to DB after the attachment was saved to the FS
        Attachment stored = createFsAttachment("A");
        stored.setTypeKey(DB_TYPE_KEY);
        stored = attachmentRepository.saveAndFlush(stored);
        Attachment other = createFsAttachment("B");

        AttachmentDto dto = attachmentMapper.toDto(stored);
        dto.setContent(null);
        dto.setDescription(UPDATED_DESCRIPTION);
        restAttachmentMockMvc.perform(put("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contentUrl").value(stored.getContentUrl()))
            .andExpect(jsonPath("$.description").value(UPDATED_DESCRIPTION));

        dto.setContentUrl(other.getContentUrl());
        restAttachmentMockMvc.perform(put("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.content.url.not.allowed"));
    }

    @Test
    @Transactional
    public void shouldAcceptStorageContentUrlInSave() {
        // LEP and addFileAttachment call save with a ready content url and without content
        Attachment stored = createFsAttachment("A");

        Attachment saved = attachmentService.save(attachmentWithContentUrl(DEFAULT_TYPE_KEY, stored.getContentUrl()));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getContentUrl()).isEqualTo(stored.getContentUrl());
    }

    @Test
    @Transactional
    public void shouldNotDeleteDirectoryByContentUrl() throws Exception {
        Attachment victim = createFsAttachment("A");
        Path victimFile = tenantRoot().resolve(StringUtils.substringAfter(victim.getContentUrl(), FILE_PREFIX));

        // content url points to the folder with the files of all attachments of the type
        Attachment attachment = attachmentRepository.saveAndFlush(
            attachmentWithContentUrl(DEFAULT_TYPE_KEY, FILE_PREFIX + entityFolder(victim)));

        restAttachmentMockMvc.perform(delete("/api/attachments/{id}", attachment.getId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.file.path"));

        assertThat(victimFile).exists();
    }

    @Test
    @Transactional
    public void shouldNotUseAttachmentNameAsFilePath() throws Exception {
        Attachment victim = createFsAttachment("victim");
        String victimPath = StringUtils.substringAfter(victim.getContentUrl(), FILE_PREFIX);
        Path victimFile = tenantRoot().resolve(victimPath);

        // the name points from the entity folder to the file of another attachment
        Attachment attachment = attachmentWithContentUrl(DEFAULT_TYPE_KEY, null);
        attachment.setName("x/../../" + victimPath);
        attachment.setValueContentType(DEFAULT_VALUE_CONTENT_TYPE);
        attachment.setContent(createContent("attacker"));

        restAttachmentMockMvc.perform(post("/api/attachments")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(attachmentMapper.toDto(attachment))))
            .andExpect(status().isCreated());

        assertThat(victimFile).hasContent("victim");
        Attachment saved = attachmentRepository.findAll().getLast();
        assertThat(saved.getContentUrl()).doesNotContain("..");
        assertThat(entityFolder(saved)).isEqualTo(entityFolder(victim));

        restAttachmentMockMvc.perform(delete("/api/attachments/{id}", saved.getId()))
            .andExpect(status().is2xxSuccessful());

        assertThat(victimFile).hasContent("victim");
    }

    private Attachment createFsAttachment(String value) {
        Attachment attachment = attachmentWithContentUrl(DEFAULT_TYPE_KEY, null);
        attachment.setContent(createContent(value));
        return attachmentService.save(attachment);
    }

    private static String entityFolder(Attachment attachment) {
        return StringUtils.substringBefore(StringUtils.substringAfter(attachment.getContentUrl(), FILE_PREFIX), "/");
    }

    @Test
    @Transactional
    public void shouldNotReadFileOutsideTenantFolder() throws Exception {
        Path probe = createProbe();
        try {
            Attachment attachment = attachmentRepository.saveAndFlush(attachmentWithContentUrl(DEFAULT_TYPE_KEY,
                FILE_PREFIX + "../" + probe.getFileName() + "/sentinel.txt"));

            restAttachmentMockMvc.perform(get("/api/attachments/{id}", attachment.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("error.file.path"));
        } finally {
            FileUtils.deleteQuietly(probe.toFile());
        }
    }

    private Attachment attachmentWithContentUrl(String typeKey, String contentUrl) {
        Attachment attachment = new Attachment();
        attachment.setTypeKey(typeKey);
        attachment.setName(DEFAULT_NAME);
        attachment.setStartDate(DEFAULT_START_DATE);
        attachment.setContentUrl(contentUrl);
        attachment.setXmEntity(xmEntity);
        return attachment;
    }

    private Path tenantRoot() {
        return Paths.get(applicationProperties.getObjectStorage().getFileRoot()).resolve("resinttest");
    }

    private Path createProbe() throws IOException {
        Path probe = tenantRoot().resolveSibling("probe-" + UUID.randomUUID());
        Files.createDirectories(probe);
        Files.writeString(probe.resolve("sentinel.txt"), "secret");
        return probe;
    }

    private static Content createContent(String value) {
        Content content = new Content();
        content.setValue(value.getBytes());
        return content;
    }

}
