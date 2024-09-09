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
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.TagService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import jakarta.persistence.EntityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.util.List;

/**
 * Extended Test class for the TagResource REST controller.
 *
 * @see TagResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class TagResourceExtendedIntTest extends AbstractSpringBootTest {

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);

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
    private TagRepository tagRepository;

    @Autowired
    private TagResource tagResource;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private TagService tagService;

    private MockMvc restTagMockMvc;

    private Tag tag;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);

        tagService = new TagService(
            tagRepository,
            permittedRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository);

        TagResource tagResourceMock = new TagResource(tagResource, tagService);
        this.restTagMockMvc = MockMvcBuilders.standaloneSetup(tagResourceMock)
                                             .setCustomArgumentResolvers(pageableArgumentResolver)
                                             .setControllerAdvice(exceptionTranslator)
                                             .setValidator(validator)
                                             .setMessageConverters(jacksonMessageConverter).build();

        tag = TagResourceIntTest.createEntity(em);

    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void checkStartDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = tagRepository.findAll().size();
        // set the field null
        tag.setStartDate(null);

        // Create the Tag.

        restTagMockMvc.perform(post("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.convertObjectToJsonBytes(tag)))
                      .andExpect(status().isCreated())
                      .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
        ;

        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeTest + 1);

        Tag testTag = tagList.get(tagList.size() - 1);
        assertThat(testTag.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void createTagWithEntryDate() throws Exception {

        int databaseSizeBeforeCreate = tagRepository.findAll().size();

        // Create the Tag
        restTagMockMvc.perform(post("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.convertObjectToJsonBytes(tag)))
                      .andExpect(status().isCreated())
                      .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()));

        // Validate the Tag in the database
        List<Tag> voteList = tagRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate + 1);

        Tag testTag = voteList.get(voteList.size() - 1);
        assertThat(testTag.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequiredInDb() throws Exception {

        Tag o = tagService.save(tag);
        // set the field null
        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(null);
        o.setStartDate(null);
        tagService.save(o);

        try {
            tagRepository.flush();
            fail("DataIntegrityViolationException exception was expected!");
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage())
                .containsIgnoringCase("NULL not allowed for column \"START_DATE\"");
        }

    }

}
