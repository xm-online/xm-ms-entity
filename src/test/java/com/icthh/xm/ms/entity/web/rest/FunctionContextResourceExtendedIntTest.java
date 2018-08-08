package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.repository.FunctionContextRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.FunctionContextSearchRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.service.impl.FunctionContextServiceImpl;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * Extended Test class for the FunctionContextResource REST controller.
 *
 * @see FunctionContextResource
 */
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class
})
public class FunctionContextResourceExtendedIntTest {

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);
    private static final Instant MOCKED_UPDATE_DATE = Instant.ofEpochMilli(84L);

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private FunctionContextRepository functionContextRepository;

    @Autowired
    private FunctionContextResource functionContextResource;

    @Autowired
    private FunctionContextSearchRepository functionContextSearchRepository;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private PermittedSearchRepository permittedSearchRepository;

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
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);
        when(startUpdateDateGenerationStrategy.generateUpdateDate()).thenReturn(MOCKED_UPDATE_DATE);

        functionContextService = new FunctionContextServiceImpl(
            functionContextRepository,
            functionContextSearchRepository,
            permittedRepository,
            permittedSearchRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository);

        FunctionContextResource functionContextResourceMock = new FunctionContextResource(functionContextService,
                                                                                          functionContextResource);
        this.restFunctionContextMockMvc = MockMvcBuilders.standaloneSetup(functionContextResourceMock)
                                                         .setCustomArgumentResolvers(pageableArgumentResolver)
                                                         .setControllerAdvice(exceptionTranslator)
                                                         .setValidator(validator)
                                                         .setMessageConverters(jacksonMessageConverter).build();

        functionContext = FunctionContextResourceIntTest.createEntity(em);

    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void checkStartDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = functionContextRepository.findAll().size();
        // set the field null
        functionContext.setStartDate(null);

        // Create the FunctionContext.

        restFunctionContextMockMvc.perform(post("/api/function-contexts")
                                               .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                               .content(TestUtil.convertObjectToJsonBytes(functionContext)))
                                  .andExpect(status().isCreated())
                                  .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
                                  .andExpect(jsonPath("$.updateDate").value(MOCKED_UPDATE_DATE.toString()))
        ;

        List<FunctionContext> functionContextList = functionContextRepository.findAll();
        assertThat(functionContextList).hasSize(databaseSizeBeforeTest + 1);

        FunctionContext testFunctionContext = functionContextList.get(functionContextList.size() - 1);
        assertThat(testFunctionContext.getStartDate()).isEqualTo(MOCKED_START_DATE);
        assertThat(testFunctionContext.getUpdateDate()).isEqualTo(MOCKED_UPDATE_DATE);

        // Validate the FunctionContext in Elasticsearch
        FunctionContext functionContextEs = functionContextSearchRepository.findOne(testFunctionContext.getId());
        assertThat(functionContextEs).isEqualToComparingFieldByField(testFunctionContext);
    }

    @Test
    @Transactional
    public void createFunctionContextWithEntryDate() throws Exception {

        int databaseSizeBeforeCreate = functionContextRepository.findAll().size();

        // Create the FunctionContext
        restFunctionContextMockMvc.perform(post("/api/function-contexts")
                                               .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                               .content(TestUtil.convertObjectToJsonBytes(functionContext)))
                                  .andExpect(status().isCreated())
                                  .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
                                  .andExpect(jsonPath("$.updateDate").value(MOCKED_UPDATE_DATE.toString()))
        ;

        // Validate the FunctionContext in the database
        List<FunctionContext> voteList = functionContextRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate + 1);

        FunctionContext testFunctionContext = voteList.get(voteList.size() - 1);
        assertThat(testFunctionContext.getStartDate()).isEqualTo(MOCKED_START_DATE);
        assertThat(testFunctionContext.getUpdateDate()).isEqualTo(MOCKED_UPDATE_DATE);

        // Validate the FunctionContext in Elasticsearch
        FunctionContext functionContextEs = functionContextSearchRepository.findOne(testFunctionContext.getId());
        assertThat(functionContextEs).isEqualToComparingFieldByField(testFunctionContext);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequiredInDb() throws Exception {

        FunctionContext o = functionContextService.save(functionContext);
        // set the field null
        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(null);
        o.setStartDate(null);
        functionContextService.save(o);

        try {
            functionContextRepository.flush();
            fail("DataIntegrityViolationException exception was expected!");
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage())
                .containsIgnoringCase("NULL not allowed for column \"START_DATE\"");
        }

    }

    @Test
    @Transactional
    public void checkStartDateIsNotRequiredInDb() throws Exception {

        // set the field null
        when(startUpdateDateGenerationStrategy.generateUpdateDate()).thenReturn(null);

        functionContextService.save(functionContext);

    }

}
