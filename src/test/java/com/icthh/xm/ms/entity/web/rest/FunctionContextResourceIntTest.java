package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.collect.ImmutableMap;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.impl.FunctionContextServiceImpl;
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
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Test class for the FunctionContextResource REST controller.
 *
 * @see FunctionContextResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class FunctionContextResourceIntTest extends AbstractJupiterSpringBootTest {

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
    private FunctionContextResource functionContextResource;

    @Autowired
    private FunctionContextRepository functionContextRepository;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private FunctionContextService functionContextService;

    private MockMvc restFunctionContextMockMvc;

    private FunctionContext functionContext;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
        });
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(DEFAULT_START_DATE);
        when(startUpdateDateGenerationStrategy.generateUpdateDate()).thenReturn(DEFAULT_UPDATE_DATE);

        functionContextService = new FunctionContextServiceImpl(functionContextRepository,
                                                                permittedRepository,
                                                                startUpdateDateGenerationStrategy,
                                                                xmEntityRepository);

        FunctionContextResource functionContextResourceMock = new FunctionContextResource(
            functionContextService, functionContextResource);
        this.restFunctionContextMockMvc = MockMvcBuilders.standaloneSetup(functionContextResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static FunctionContext createEntity(EntityManager em) {
        FunctionContext functionContext = new FunctionContext()
            .key(DEFAULT_KEY)
            .typeKey(DEFAULT_TYPE_KEY)
            .description(DEFAULT_DESCRIPTION)
            .startDate(DEFAULT_START_DATE)
            .updateDate(DEFAULT_UPDATE_DATE)
            .endDate(DEFAULT_END_DATE)
            .data(DEFAULT_DATA);
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();
        functionContext.setXmEntity(xmEntity);
        return functionContext;
    }

    @BeforeEach
    public void initTest() {
        // functionContextSearchRepository.deleteAll();
        functionContext = createEntity(em);
    }

    @AfterEach
    public void tearDown() {
        lepManager.endThreadContext();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void createFunctionContext() throws Exception {
        int databaseSizeBeforeCreate = functionContextRepository.findAll().size();

        // Create the FunctionContext
        restFunctionContextMockMvc.perform(post("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(functionContext)))
            .andExpect(status().isCreated());

        // Validate the FunctionContext in the database
        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeCreate + 1);
        FunctionContext testFunctionContext = functionContextList.get(functionContextList.size() - 1);
        assertThat(testFunctionContext.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testFunctionContext.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testFunctionContext.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testFunctionContext.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testFunctionContext.getUpdateDate()).isEqualTo(DEFAULT_UPDATE_DATE);
        assertThat(testFunctionContext.getEndDate()).isEqualTo(DEFAULT_END_DATE);
        assertThat(testFunctionContext.getData()).isEqualTo(DEFAULT_DATA);
    }

    @Test
    @Transactional
    public void createFunctionContextWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = functionContextRepository.findAll().size();

        // Create the FunctionContext with an existing ID
        functionContext.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restFunctionContextMockMvc.perform(post("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(functionContext)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = functionContextRepository.findAll().size();
        // set the field null
        functionContext.setKey(null);

        // Create the FunctionContext, which fails.

        restFunctionContextMockMvc.perform(post("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(functionContext)))
            .andExpect(status().isBadRequest());

        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = functionContextRepository.findAll().size();
        // set the field null
        functionContext.setTypeKey(null);

        // Create the FunctionContext, which fails.

        restFunctionContextMockMvc.perform(post("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(functionContext)))
            .andExpect(status().isBadRequest());

        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Disabled("see FunctionContextResourceExtendedIntTest.checkStartDateIsNotRequired instead")
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = functionContextRepository.findAll().size();
        // set the field null
        functionContext.setStartDate(null);

        // Create the FunctionContext, which fails.

        restFunctionContextMockMvc.perform(post("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(functionContext)))
            .andExpect(status().isBadRequest());

        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllFunctionContexts() throws Exception {
        // Initialize the database
        functionContextRepository.saveAndFlush(functionContext);

        // Get all the functionContextList
        restFunctionContextMockMvc.perform(get("/api/function-contexts?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(functionContext.getId().intValue())))
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
    public void getFunctionContext() throws Exception {
        // Initialize the database
        functionContext = functionContextRepository.saveAndFlush(functionContext);

        // Get the functionContext
        restFunctionContextMockMvc.perform(get("/api/function-contexts/{id}", functionContext.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(functionContext.getId().intValue()))
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
    public void getNonExistingFunctionContext() throws Exception {
        // Get the functionContext
        restFunctionContextMockMvc.perform(get("/api/function-contexts/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateFunctionContext() throws Exception {
        // Initialize the database
        functionContextService.save(functionContext);

        int databaseSizeBeforeUpdate = functionContextRepository.findAll().size();

        // Update the functionContext
        FunctionContext updatedFunctionContext = functionContextRepository.findById(functionContext.getId())
            .orElseThrow(NullPointerException::new);

        em.detach(updatedFunctionContext);

        updatedFunctionContext
            .key(UPDATED_KEY)
            .typeKey(UPDATED_TYPE_KEY)
            .description(UPDATED_DESCRIPTION)
            .startDate(UPDATED_START_DATE)
            .updateDate(UPDATED_UPDATE_DATE)
            .endDate(UPDATED_END_DATE)
            .data(UPDATED_DATA);

        restFunctionContextMockMvc.perform(put("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedFunctionContext)))
            .andExpect(status().isOk());

        // Validate the FunctionContext in the database
        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeUpdate);
        FunctionContext testFunctionContext = functionContextList.get(functionContextList.size() - 1);
        assertThat(testFunctionContext.getKey()).isEqualTo(UPDATED_KEY);
        assertThat(testFunctionContext.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testFunctionContext.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testFunctionContext.getStartDate()).isEqualTo(DEFAULT_START_DATE);
        assertThat(testFunctionContext.getUpdateDate()).isEqualTo(DEFAULT_UPDATE_DATE);
        assertThat(testFunctionContext.getEndDate()).isEqualTo(UPDATED_END_DATE);
        assertThat(testFunctionContext.getData()).isEqualTo(UPDATED_DATA);
    }

    @Test
    @Transactional
    public void updateNonExistingFunctionContext() throws Exception {
        int databaseSizeBeforeUpdate = functionContextRepository.findAll().size();

        // Create the FunctionContext

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restFunctionContextMockMvc.perform(put("/api/function-contexts")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(functionContext)))
            .andExpect(status().isCreated());

        // Validate the FunctionContext in the database
        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteFunctionContext() throws Exception {
        // Initialize the database
        functionContextService.save(functionContext);

        int databaseSizeBeforeDelete = functionContextRepository.findAll().size();

        // Get the functionContext
        restFunctionContextMockMvc.perform(delete("/api/function-contexts/{id}", functionContext.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(FunctionContext.class);
        FunctionContext functionContext1 = new FunctionContext();
        functionContext1.setId(1L);
        FunctionContext functionContext2 = new FunctionContext();
        functionContext2.setId(functionContext1.getId());
        assertThat(functionContext1).isEqualTo(functionContext2);
        functionContext2.setId(2L);
        assertThat(functionContext1).isNotEqualTo(functionContext2);
        functionContext1.setId(null);
        assertThat(functionContext1).isNotEqualTo(functionContext2);
    }
}
