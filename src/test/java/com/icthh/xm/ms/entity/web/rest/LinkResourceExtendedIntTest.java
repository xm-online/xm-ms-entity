package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.repository.LinkPermittedRepository;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.PermittedSearchRepository;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;

/**
 * Extended Test class for the LinkResource REST controller.
 *
 * @see LinkResource
 */
@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class LinkResourceExtendedIntTest extends AbstractSpringBootTest {

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
    private LinkRepository linkRepository;

    @Autowired
    private LinkResource linkResource;

    @Autowired
    private LinkPermittedRepository permittedRepository;

    @Autowired
    private PermittedSearchRepository permittedSearchRepository;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private LinkService linkService;

    private MockMvc restLinkMockMvc;

    private Link link;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);

        linkService = new LinkService(
            linkRepository,
            permittedRepository,
            permittedSearchRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository);

        LinkResource linkResourceMock = new LinkResource(linkService, linkResource);
        this.restLinkMockMvc = MockMvcBuilders.standaloneSetup(linkResourceMock)
                                              .setCustomArgumentResolvers(pageableArgumentResolver)
                                              .setControllerAdvice(exceptionTranslator)
                                              .setValidator(validator)
                                              .setMessageConverters(jacksonMessageConverter).build();

        link = LinkResourceIntTest.createEntity(em);

        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void checkStartDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = linkRepository.findAll().size();
        // set the field null
        link.setStartDate(null);

        // Create the Link.

        restLinkMockMvc.perform(post("/api/links")
                                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                    .content(TestUtil.convertObjectToJsonBytes(link)))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
        ;

        List<Link> linkList = linkRepository.findAll();
        assertThat(linkList).hasSize(databaseSizeBeforeTest + 1);

        Link testLink = linkList.get(linkList.size() - 1);
        assertThat(testLink.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void createLinkWithEntryDate() throws Exception {

        int databaseSizeBeforeCreate = linkRepository.findAll().size();

        // Create the Link
        restLinkMockMvc.perform(post("/api/links")
                                    .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                    .content(TestUtil.convertObjectToJsonBytes(link)))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()));

        // Validate the Link in the database
        List<Link> voteList = linkRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate + 1);

        Link testLink = voteList.get(voteList.size() - 1);
        assertThat(testLink.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequiredInDb() {

        Link o = linkService.save(link);
        // set the field null
        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(null);
        o.setStartDate(null);
        linkService.save(o);

        try {
            linkRepository.flush();
            fail("DataIntegrityViolationException exception was expected!");
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage())
                .containsIgnoringCase("NULL not allowed for column \"START_DATE\"");
        }

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllLinks() throws Exception {
        // Initialize the database
        link.getTarget().setCreatedBy("admin");
        linkRepository.saveAndFlush(link);

        // Get all the linkList
        restLinkMockMvc.perform(get("/api/links?sort=id,desc"))
                       .andExpect(status().isOk())
                       .andDo(this::printMvcResult)
                       .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                       .andExpect(jsonPath("$", hasSize(1)))
                       .andExpect(jsonPath("$.[*].id").value(everyItem(is(link.getId().intValue()))))
                       .andExpect(jsonPath("$.[*].typeKey").value(everyItem(is(LinkResourceIntTest.DEFAULT_TYPE_KEY))))
                       .andExpect(jsonPath("$.[*].name").value(everyItem(is(LinkResourceIntTest.DEFAULT_NAME))))
                       .andExpect(jsonPath("$.[*].description").value(everyItem(is(LinkResourceIntTest.DEFAULT_DESCRIPTION))))
                       .andExpect(jsonPath("$.[*].startDate").value(everyItem(is(LinkResourceIntTest.DEFAULT_START_DATE.toString()))))
                       .andExpect(jsonPath("$.[*].endDate").value(everyItem(is(LinkResourceIntTest.DEFAULT_END_DATE.toString()))))

                       .andExpect(jsonPath("$.[*].target").exists())
                       .andExpect(jsonPath("$.[*].target.id").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.key").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.typeKey").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.stateKey").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.name").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.startDate").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.endDate").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.updateDate").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.description").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.createdBy").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.removed").value(everyItem(notNullValue())))
                       .andExpect(jsonPath("$.[*].target.data").exists())
                       .andExpect(jsonPath("$.[*].target.data.AAAAAAAAAA").value(everyItem(notNullValue())))

                       .andExpect(jsonPath("$.[*].target.avatarUrlRelative").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.avatarUrlFull").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.version").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.targets").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.sources").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.attachments").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.locations").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.tags").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.calendars").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.ratings").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.comments").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.votes").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.functionContexts").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.events").doesNotExist())
                       .andExpect(jsonPath("$.[*].target.uniqueFields").doesNotExist())
        ;
    }

    @Test
    @Transactional
    public void getLink() throws Exception {
        // Initialize the database
        link.getTarget().setCreatedBy("admin");
        linkRepository.saveAndFlush(link);

        assertFalse(link.getTarget().isRemoved());

        // Get the link
        restLinkMockMvc.perform(get("/api/links/{id}", link.getId()))
                       .andExpect(status().isOk())
                       .andDo(this::printMvcResult)
                       .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                       .andExpect(jsonPath("$.id").value(link.getId().intValue()))
                       .andExpect(jsonPath("$.typeKey").value(LinkResourceIntTest.DEFAULT_TYPE_KEY))
                       .andExpect(jsonPath("$.name").value(LinkResourceIntTest.DEFAULT_NAME))
                       .andExpect(jsonPath("$.description").value(LinkResourceIntTest.DEFAULT_DESCRIPTION))
                       .andExpect(jsonPath("$.startDate").value(LinkResourceIntTest.DEFAULT_START_DATE.toString()))
                       .andExpect(jsonPath("$.endDate").value(LinkResourceIntTest.DEFAULT_END_DATE.toString()))
                       .andExpect(jsonPath("$.target").exists())
                       .andExpect(jsonPath("$.target.id").value(notNullValue()))
                       .andExpect(jsonPath("$.target.key").value(notNullValue()))
                       .andExpect(jsonPath("$.target.typeKey").value(notNullValue()))
                       .andExpect(jsonPath("$.target.stateKey").value(notNullValue()))
                       .andExpect(jsonPath("$.target.name").value(notNullValue()))
                       .andExpect(jsonPath("$.target.startDate").value(notNullValue()))
                       .andExpect(jsonPath("$.target.startDate").value(notNullValue()))
                       .andExpect(jsonPath("$.target.updateDate").value(notNullValue()))
                       .andExpect(jsonPath("$.target.description").value(notNullValue()))
                       .andExpect(jsonPath("$.target.createdBy").value(notNullValue()))
                       .andExpect(jsonPath("$.target.removed").value(notNullValue()))
                       .andExpect(jsonPath("$.target.data").exists())
                       .andExpect(jsonPath("$.target.data.AAAAAAAAAA").value(notNullValue()))

                       .andExpect(jsonPath("$.target.avatarUrlRelative").doesNotExist())
                       .andExpect(jsonPath("$.target.avatarUrlFull").doesNotExist())
                       .andExpect(jsonPath("$.target.version").doesNotExist())
                       .andExpect(jsonPath("$.target.targets").doesNotExist())
                       .andExpect(jsonPath("$.target.targets").doesNotExist())
                       .andExpect(jsonPath("$.target.sources").doesNotExist())
                       .andExpect(jsonPath("$.target.attachments").doesNotExist())
                       .andExpect(jsonPath("$.target.locations").doesNotExist())
                       .andExpect(jsonPath("$.target.tags").doesNotExist())
                       .andExpect(jsonPath("$.target.calendars").doesNotExist())
                       .andExpect(jsonPath("$.target.ratings").doesNotExist())
                       .andExpect(jsonPath("$.target.comments").doesNotExist())
                       .andExpect(jsonPath("$.target.votes").doesNotExist())
                       .andExpect(jsonPath("$.target.functionContexts").doesNotExist())
                       .andExpect(jsonPath("$.target.events").doesNotExist())
                       .andExpect(jsonPath("$.target.uniqueFields").doesNotExist())
        ;
    }

    @SneakyThrows
    private void printMvcResult(MvcResult result) {
        log.info("MVC result: {}", result.getResponse().getContentAsString());
    }

}
