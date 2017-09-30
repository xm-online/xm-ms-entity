package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableMap;
import com.icthh.lep.api.LepManager;
import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.config.LepConfig;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.lep.XmLepConstants;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.api.XmEntityServiceResolver;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

/**
 * Test class for the XmEntityResource REST controller.
 *
 * @see XmEntityResource
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class, LepConfig.class})
public class XmEntityResourceIntTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE_KEY = "ACCOUNT.ADMIN";
    private static final String UPDATED_TYPE_KEY = "ACCOUNT.OWNER";

    private static final String DEFAULT_STATE_KEY = "STATE2";
    private static final String UPDATED_STATE_KEY = "STATE3";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATE_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATE_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_AVATAR_URL = "http://hello.rgw.icthh.test/aaaaa.jpg";
    private static final String UPDATED_AVATAR_URL = "http://hello.rgw.icthh.test/bbbbb.jpg";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Map<String, Object> DEFAULT_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "BBBBBBBBBB").build();
    private static final Map<String, Object> UPDATED_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "CCCCCCCCCC").build();

    private static final Boolean DEFAULT_REMOVED = false;
    private static final Boolean UPDATED_REMOVED = true;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private XmEntityServiceResolver xmEntityService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileEventProducer profileEventProducer;

    @Autowired
    private XmEntitySearchRepository xmEntitySearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private LepManager lepManager;

    private MockMvc restXmEntityMockMvc;

    private XmEntity xmEntity;

    @Autowired
    private Validator validator;

    @Before
    public void setup() {
        TenantContext.setCurrent("RESINTTEST");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(XmLepConstants.CONTEXT_KEY_TENANT, TenantContext.getCurrent());
        });

        MockitoAnnotations.initMocks(this);
        XmEntityResource xmEntityResource = new XmEntityResource(xmEntityService, profileService, profileEventProducer);
        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(xmEntityResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();

        xmEntity = createEntity(em);
    }

    @After
    public void finalize() {
        lepManager.endThreadContext();
        TenantContext.setCurrent("XM");
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it, if they test an entity which requires
     * the current entity.
     */
    public static XmEntity createEntity(EntityManager em) {
        XmEntity xmEntity = new XmEntity()
            .key(DEFAULT_KEY)
            .typeKey(DEFAULT_TYPE_KEY)
            .stateKey(DEFAULT_STATE_KEY)
            .name(DEFAULT_NAME)
            .startDate(DEFAULT_START_DATE)
            .updateDate(DEFAULT_UPDATE_DATE)
            .endDate(DEFAULT_END_DATE)
            .avatarUrl(DEFAULT_AVATAR_URL)
            .description(DEFAULT_DESCRIPTION)
            .data(DEFAULT_DATA)
            .removed(DEFAULT_REMOVED);
        return xmEntity;
    }

    @Before
    public void initTest() {
        //    xmEntitySearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void createXmEntity() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        // Create the XmEntity
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isCreated());

        // Validate the XmEntity in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate + 1);
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
        assertThat(testXmEntity.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testXmEntity.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testXmEntity.getStateKey()).isEqualTo(DEFAULT_STATE_KEY);
        assertThat(testXmEntity.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testXmEntity.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testXmEntity.getUpdateDate()).isEqualTo(DEFAULT_UPDATE_DATE);
        assertThat(testXmEntity.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testXmEntity.getAvatarUrl()).contains("aaaaa.jpg");
        assertThat(testXmEntity.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testXmEntity.getData()).isEqualTo(DEFAULT_DATA);
        assertThat(testXmEntity.isRemoved()).isEqualTo(DEFAULT_REMOVED);

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(testXmEntity.getId());
        assertThat(xmEntityEs).isEqualToComparingFieldByField(testXmEntity);
    }

    @Test
    @Transactional
    public void createXmEntityWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        // Create the XmEntity with an existing ID
        xmEntity.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void createXmEntityTenantWithWhitespace() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();

        XmEntity tenant = createEntity(em);
        tenant.setTypeKey(Constants.TENANT_TYPE_KEY);
        tenant.setName("test name");

        // An entity with an existing ID cannot be created, so this API call must fail
        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(tenant)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setKey(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("key"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNullTenantAware"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setTypeKey(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("typeKey"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkNameIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setName(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNullTenantAware"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setStartDate(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNullTenantAware"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUpdateDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmEntityRepository.findAll().size();
        // set the field null
        xmEntity.setUpdateDate(null);

        // Create the XmEntity, which fails.

        restXmEntityMockMvc.perform(post("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("xmEntity"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("updateDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNullTenantAware"))
        ;

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeTest);
    }


    @Test
    @Transactional
    public void getAllXmEntities() throws Exception {
        // Initialize the database
        xmEntityRepository.saveAndFlush(xmEntity);

        // Get all the xmEntityList
        restXmEntityMockMvc.perform(get("/api/xm-entities?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(DEFAULT_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].avatarUrl").value(hasItem(containsString("aaaaa.jpg"))))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(DEFAULT_REMOVED.booleanValue())))

            // check that tags are not returned foe XmEntities collection
            .andExpect(jsonPath("$.[*].tags.id").value(everyItem(nullValue())));
    }


    @Test
    @Transactional
    public void getXmEntity() throws Exception {
        // Initialize the database
        xmEntityRepository.saveAndFlush(xmEntity);

        // Get the xmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities/{id}", xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(xmEntity.getId().intValue()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY.toString()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.stateKey").value(DEFAULT_STATE_KEY.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.updateDate").value(DEFAULT_UPDATE_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.avatarUrl").value(containsString("aaaaa.jpg")))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"))
            .andExpect(jsonPath("$.removed").value(DEFAULT_REMOVED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingXmEntity() throws Exception {
        // Get the xmEntity
        restXmEntityMockMvc.perform(get("/api/xm-entities/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateXmEntity() throws Exception {
        // Initialize the database
        xmEntityService.save(xmEntity);

        int databaseSizeBeforeUpdate = xmEntityRepository.findAll().size();

        // Update the xmEntity
        XmEntity updatedXmEntity = xmEntityRepository.findOne(xmEntity.getId());
        updatedXmEntity
            .key(UPDATED_KEY)
            .typeKey(UPDATED_TYPE_KEY)
            .stateKey(UPDATED_STATE_KEY)
            .name(UPDATED_NAME)
            .startDate(UPDATED_START_DATE)
            .updateDate(UPDATED_UPDATE_DATE)
            .endDate(UPDATED_END_DATE)
            .avatarUrl(UPDATED_AVATAR_URL)
            .description(UPDATED_DESCRIPTION)
            .data(UPDATED_DATA)
            .removed(UPDATED_REMOVED);

        restXmEntityMockMvc.perform(put("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedXmEntity)))
            .andExpect(status().isOk());

        // Validate the XmEntity in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeUpdate);
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
        assertThat(testXmEntity.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testXmEntity.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testXmEntity.getStateKey()).isEqualTo(UPDATED_STATE_KEY);
        assertThat(testXmEntity.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testXmEntity.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testXmEntity.getUpdateDate()).isEqualTo(UPDATED_UPDATE_DATE);
        assertThat(testXmEntity.getEndDate()).isEqualTo(UPDATED_END_DATE);
        assertThat(testXmEntity.getAvatarUrl()).contains("bbbbb.jpg");
        assertThat(testXmEntity.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testXmEntity.getData()).isEqualTo(UPDATED_DATA);
        assertThat(testXmEntity.isRemoved()).isEqualTo(UPDATED_REMOVED);

        // Validate the XmEntity in Elasticsearch
        XmEntity xmEntityEs = xmEntitySearchRepository.findOne(testXmEntity.getId());
        assertThat(xmEntityEs).isEqualToIgnoringGivenFields(testXmEntity, "avatarUrl");
    }

    @Test
    @Transactional
    public void updateNonExistingXmEntity() throws Exception {
        int databaseSizeBeforeUpdate = xmEntityRepository.findAll().size();

        // Create the XmEntity

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restXmEntityMockMvc.perform(put("/api/xm-entities")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isCreated());

        // Validate the XmEntity in the database
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteXmEntity() throws Exception {
        // Initialize the database
        xmEntityService.save(xmEntity);

        int databaseSizeBeforeDelete = xmEntityRepository.findAll().size();

        // Get the xmEntity
        restXmEntityMockMvc.perform(delete("/api/xm-entities/{id}", xmEntity.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate Elasticsearch is empty
        boolean xmEntityExistsInEs = xmEntitySearchRepository.exists(xmEntity.getId());
        assertThat(xmEntityExistsInEs).isFalse();

        // Validate the database is empty
        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchXmEntity() throws Exception {
        // Initialize the database
        xmEntityService.save(xmEntity);

        // Search the xmEntity
        restXmEntityMockMvc.perform(get("/api/_search/xm-entities?query=id:" + xmEntity.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmEntity.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].stateKey").value(hasItem(DEFAULT_STATE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(DEFAULT_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].avatarUrl").value(hasItem(containsString("aaaaa.jpg"))))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)))
            .andExpect(jsonPath("$.[*].removed").value(hasItem(DEFAULT_REMOVED.booleanValue())));
    }

    @Test
    @Transactional
    public void changeStateError() throws Exception {
        XmEntitySpecService xmEntitySpecService = Mockito.mock(XmEntitySpecService.class);
        StateSpec nextSpec = new StateSpec();
        nextSpec.setKey("NEXT_STATE");
        when(xmEntitySpecService.nextStates(eq(DEFAULT_TYPE_KEY), eq(DEFAULT_STATE_KEY)))
            .thenReturn(Collections.singletonList(nextSpec));

        XmEntity tenant = createEntity(em);
        xmEntityService.save(tenant);

        restXmEntityMockMvc.perform(
            put("/api/xm-entities/{idOrKey}/states/{stateKey}", tenant.getId(),
                "INVALID_NEXT_STATE").contentType(
                TestUtil.APPLICATION_JSON_UTF8)).andExpect(
            status().is5xxServerError());
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(XmEntity.class);
        XmEntity xmEntity1 = new XmEntity();
        xmEntity1.setId(1L);
        XmEntity xmEntity2 = new XmEntity();
        xmEntity2.setId(xmEntity1.getId());
        assertThat(xmEntity1).isEqualTo(xmEntity2);
        xmEntity2.setId(2L);
        assertThat(xmEntity1).isNotEqualTo(xmEntity2);
        xmEntity1.setId(null);
        assertThat(xmEntity1).isNotEqualTo(xmEntity2);
    }

}
