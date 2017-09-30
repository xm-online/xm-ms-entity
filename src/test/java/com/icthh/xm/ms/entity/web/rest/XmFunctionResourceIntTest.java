package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
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
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.lep.XmLepConstants;
import com.icthh.xm.ms.entity.repository.XmFunctionRepository;
import com.icthh.xm.ms.entity.repository.search.XmFunctionSearchRepository;
import com.icthh.xm.ms.entity.service.api.XmFunctionServiceResolver;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

/**
 * Test class for the XmFunctionResource REST controller.
 *
 * @see XmFunctionResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class XmFunctionResourceIntTest {

    private static final String DEFAULT_KEY = "AAAAAAAAAA";
    private static final String UPDATED_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_TYPE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_UPDATE_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_UPDATE_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Map<String, Object> DEFAULT_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "BBBBBBBBBB").build();
    private static final Map<String, Object> UPDATED_DATA = ImmutableMap.<String, Object>builder()
        .put("AAAAAAAAAA", "CCCCCCCCCC").build();

    @Autowired
    private XmFunctionRepository xmFunctionRepository;

    @Autowired
    private XmFunctionServiceResolver xmFunctionService;

    @Autowired
    private XmFunctionSearchRepository xmFunctionSearchRepository;

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

    private MockMvc restXmFunctionMockMvc;

    private XmFunction xmFunction;

    @Before
    public void setup() {
        TenantContext.setCurrent("RESINTTEST");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(XmLepConstants.CONTEXT_KEY_TENANT, TenantContext.getCurrent());
        });

        //xmFunctionSearchRepository.deleteAll();
        MockitoAnnotations.initMocks(this);
        XmFunctionResource xmFunctionResource = new XmFunctionResource(xmFunctionService);
        this.restXmFunctionMockMvc = MockMvcBuilders.standaloneSetup(xmFunctionResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        xmFunction = createEntity(em);

    }

    @After
    public void finalize() {
        lepManager.endThreadContext();
        TenantContext.setCurrent("XM");
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static XmFunction createEntity(EntityManager em) {
        XmFunction xmFunction = new XmFunction()
            .key(DEFAULT_KEY)
            .typeKey(DEFAULT_TYPE_KEY)
            .description(DEFAULT_DESCRIPTION)
            .startDate(DEFAULT_START_DATE)
            .updateDate(DEFAULT_UPDATE_DATE)
            .endDate(DEFAULT_END_DATE)
            .data(DEFAULT_DATA);
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity(em);
        em.persist(xmEntity);
        em.flush();
        xmFunction.setXmEntity(xmEntity);
        return xmFunction;
    }

    @Test
    @Transactional
    public void createXmFunction() throws Exception {
        int databaseSizeBeforeCreate = xmFunctionRepository.findAll().size();

        // Create the XmFunction
        restXmFunctionMockMvc.perform(post("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmFunction)))
            .andExpect(status().isCreated());

        // Validate the XmFunction in the database
        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeCreate + 1);
        XmFunction testXmFunction = xmFunctionList.get(xmFunctionList.size() - 1);
        assertThat(testXmFunction.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testXmFunction.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testXmFunction.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testXmFunction.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testXmFunction.getUpdateDate()).isEqualTo(DEFAULT_UPDATE_DATE);
        assertThat(testXmFunction.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testXmFunction.getData()).isEqualTo(DEFAULT_DATA);

        // Validate the XmFunction in Elasticsearch
        XmFunction xmFunctionEs = xmFunctionSearchRepository.findOne(testXmFunction.getId());
        assertThat(xmFunctionEs).isEqualToComparingFieldByField(testXmFunction);
    }

    @Test
    @Transactional
    public void createXmFunctionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = xmFunctionRepository.findAll().size();

        // Create the XmFunction with an existing ID
        xmFunction.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restXmFunctionMockMvc.perform(post("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmFunction)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmFunctionRepository.findAll().size();
        // set the field null
        xmFunction.setKey(null);

        // Create the XmFunction, which fails.

        restXmFunctionMockMvc.perform(post("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmFunction)))
            .andExpect(status().isBadRequest());

        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmFunctionRepository.findAll().size();
        // set the field null
        xmFunction.setTypeKey(null);

        // Create the XmFunction, which fails.

        restXmFunctionMockMvc.perform(post("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmFunction)))
            .andExpect(status().isBadRequest());

        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = xmFunctionRepository.findAll().size();
        // set the field null
        xmFunction.setStartDate(null);

        // Create the XmFunction, which fails.

        restXmFunctionMockMvc.perform(post("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmFunction)))
            .andExpect(status().isBadRequest());

        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllXmFunctions() throws Exception {
        // Initialize the database
        xmFunctionRepository.saveAndFlush(xmFunction);

        // Get all the xmFunctionList
        restXmFunctionMockMvc.perform(get("/api/xm-functions?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmFunction.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(DEFAULT_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)));
    }

    @Test
    @Transactional
    public void getXmFunction() throws Exception {
        // Initialize the database
        xmFunctionRepository.saveAndFlush(xmFunction);

        // Get the xmFunction
        restXmFunctionMockMvc.perform(get("/api/xm-functions/{id}", xmFunction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(xmFunction.getId().intValue()))
            .andExpect(jsonPath("$.key").value(DEFAULT_KEY.toString()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()))
            .andExpect(jsonPath("$.updateDate").value(DEFAULT_UPDATE_DATE.toString()))
            .andExpect(jsonPath("$.endDate").value(DEFAULT_END_DATE.toString()))
            .andExpect(jsonPath("$.data.AAAAAAAAAA").value("BBBBBBBBBB"));
    }

    @Test
    @Transactional
    public void getNonExistingXmFunction() throws Exception {
        // Get the xmFunction
        restXmFunctionMockMvc.perform(get("/api/xm-functions/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateXmFunction() throws Exception {
        // Initialize the database
        xmFunctionService.save(xmFunction);

        int databaseSizeBeforeUpdate = xmFunctionRepository.findAll().size();

        // Update the xmFunction
        XmFunction updatedXmFunction = xmFunctionRepository.findOne(xmFunction.getId());
        updatedXmFunction
            .key(UPDATED_KEY)
            .typeKey(UPDATED_TYPE_KEY)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .updateDate(UPDATED_UPDATE_DATE)
            .endDate(UPDATED_END_DATE)
            .data(UPDATED_DATA);

        restXmFunctionMockMvc.perform(put("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedXmFunction)))
            .andExpect(status().isOk());

        // Validate the XmFunction in the database
        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeUpdate);
        XmFunction testXmFunction = xmFunctionList.get(xmFunctionList.size() - 1);
        assertThat(testXmFunction.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testXmFunction.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testXmFunction.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testXmFunction.getStartDate()).isEqualTo(UPDATED_START_DATE);
        assertThat(testXmFunction.getUpdateDate()).isEqualTo(UPDATED_UPDATE_DATE);
        assertThat(testXmFunction.getEndDate()).isEqualTo(UPDATED_END_DATE);
        assertThat(testXmFunction.getData()).isEqualTo(UPDATED_DATA);

        // Validate the XmFunction in Elasticsearch
        XmFunction xmFunctionEs = xmFunctionSearchRepository.findOne(testXmFunction.getId());
        assertThat(xmFunctionEs).isEqualToComparingFieldByField(testXmFunction);
    }

    @Test
    @Transactional
    public void updateNonExistingXmFunction() throws Exception {
        int databaseSizeBeforeUpdate = xmFunctionRepository.findAll().size();

        // Create the XmFunction

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restXmFunctionMockMvc.perform(put("/api/xm-functions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(xmFunction)))
            .andExpect(status().isCreated());

        // Validate the XmFunction in the database
        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteXmFunction() throws Exception {
        // Initialize the database
        xmFunctionService.save(xmFunction);

        int databaseSizeBeforeDelete = xmFunctionRepository.findAll().size();

        // Get the xmFunction
        restXmFunctionMockMvc.perform(delete("/api/xm-functions/{id}", xmFunction.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate Elasticsearch is empty
        boolean xmFunctionExistsInEs = xmFunctionSearchRepository.exists(xmFunction.getId());
        assertThat(xmFunctionExistsInEs).isFalse();

        // Validate the database is empty
        List<XmFunction> xmFunctionList = xmFunctionRepository.findAll();
        assertThat(xmFunctionList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchXmFunction() throws Exception {
        // Initialize the database
        xmFunctionService.save(xmFunction);

        // Search the xmFunction
        restXmFunctionMockMvc.perform(get("/api/_search/xm-functions?query=id:" + xmFunction.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(xmFunction.getId().intValue())))
            .andExpect(jsonPath("$.[*].key").value(hasItem(DEFAULT_KEY.toString())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())))
            .andExpect(jsonPath("$.[*].updateDate").value(hasItem(DEFAULT_UPDATE_DATE.toString())))
            .andExpect(jsonPath("$.[*].endDate").value(hasItem(DEFAULT_END_DATE.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA)));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(XmFunction.class);
        XmFunction xmFunction1 = new XmFunction();
        xmFunction1.setId(1L);
        XmFunction xmFunction2 = new XmFunction();
        xmFunction2.setId(xmFunction1.getId());
        assertThat(xmFunction1).isEqualTo(xmFunction2);
        xmFunction2.setId(2L);
        assertThat(xmFunction1).isNotEqualTo(xmFunction2);
        xmFunction1.setId(null);
        assertThat(xmFunction1).isNotEqualTo(xmFunction2);
    }

    @Test
    @Transactional
    public void executeActivateFunction() throws Exception {
        TenantContext.setCurrent("XM");
        String functionKey = "ACCOUNT.VERIFY-CONTACT-DATA";

        Map<String, Object> context = new HashMap<>();
        context.put("verificationCode", "1234");
        context.put("phone", "1234567890");

        restXmFunctionMockMvc.perform(
                        post(String.format("/api/xm-functions/call/%s", functionKey)).contentType(
                                        TestUtil.APPLICATION_JSON_UTF8).content(
                                        TestUtil.convertObjectToJsonBytes(context))).andExpect(
                        status().is5xxServerError());
    }
}
