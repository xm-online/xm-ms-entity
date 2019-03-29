package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
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
import javax.persistence.EntityManager;

/**
 * Extended Test class for the LinkResource REST controller.
 *
 * @see LinkResource
 */
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
    private XmEntitySpecService xmEntitySpecService;

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
            xmEntityRepository, xmEntitySpecService);

        LinkResource linkResourceMock = new LinkResource(linkService, linkResource);
        this.restLinkMockMvc = MockMvcBuilders.standaloneSetup(linkResourceMock)
                                              .setCustomArgumentResolvers(pageableArgumentResolver)
                                              .setControllerAdvice(exceptionTranslator)
                                              .setValidator(validator)
                                              .setMessageConverters(jacksonMessageConverter).build();

        link = LinkResourceIntTest.createEntity(em);

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
    public void checkStartDateIsRequiredInDb() throws Exception {

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

}
