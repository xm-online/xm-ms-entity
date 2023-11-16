package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.RatingRepository;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.RatingService;
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
 * Extended Test class for the RatingResource REST controller.
 *
 * @see RatingResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class RatingResourceExtendedIntTest extends AbstractSpringBootTest {

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
    private RatingRepository ratingRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private RatingResource ratingResource;

    @Autowired
    private PermittedRepository permittedRepository;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private RatingService ratingService;

    private MockMvc restRatingMockMvc;

    private Rating rating;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(MOCKED_START_DATE);

        ratingService = new RatingService(
            ratingRepository,
            voteRepository,
            permittedRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository);

        RatingResource ratingResourceMock = new RatingResource(ratingService, ratingResource);
        this.restRatingMockMvc = MockMvcBuilders.standaloneSetup(ratingResourceMock)
                                                .setCustomArgumentResolvers(pageableArgumentResolver)
                                                .setControllerAdvice(exceptionTranslator)
                                                .setValidator(validator)
                                                .setMessageConverters(jacksonMessageConverter).build();

        rating = RatingResourceIntTest.createEntity(em);

    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    @Transactional
    public void checkStartDateIsNotRequired() throws Exception {
        int databaseSizeBeforeTest = ratingRepository.findAll().size();
        // set the field null
        rating.setStartDate(null);

        // Create the Rating.

        restRatingMockMvc.perform(post("/api/ratings")
                                      .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                      .content(TestUtil.convertObjectToJsonBytes(rating)))
                         .andExpect(status().isCreated())
                         .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()))
        ;

        List<Rating> ratingList = ratingRepository.findAll();
        assertThat(ratingList).hasSize(databaseSizeBeforeTest + 1);

        Rating testRating = ratingList.get(ratingList.size() - 1);
        assertThat(testRating.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void createRatingWithEntryDate() throws Exception {

        int databaseSizeBeforeCreate = ratingRepository.findAll().size();

        // Create the Rating
        restRatingMockMvc.perform(post("/api/ratings")
                                      .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                      .content(TestUtil.convertObjectToJsonBytes(rating)))
                         .andExpect(status().isCreated())
                         .andExpect(jsonPath("$.startDate").value(MOCKED_START_DATE.toString()));

        // Validate the Rating in the database
        List<Rating> voteList = ratingRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate + 1);

        Rating testRating = voteList.get(voteList.size() - 1);
        assertThat(testRating.getStartDate()).isEqualTo(MOCKED_START_DATE);
    }

    @Test
    @Transactional
    public void checkStartDateIsRequiredInDb() throws Exception {

        Rating o = ratingService.save(rating);
        // set the field null
        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(null);
        o.setStartDate(null);
        ratingService.save(o);

        try {
            ratingRepository.flush();
            fail("DataIntegrityViolationException exception was expected!");
        } catch (DataIntegrityViolationException e) {
            assertThat(e.getMostSpecificCause().getMessage())
                .containsIgnoringCase("NULL not allowed for column \"START_DATE\"");
        }

    }

    @Test
    @Transactional
    public void testCountByRatingId() throws Exception {
        Vote vote1 = new Vote()
            .userKey("uk1")
            .value(5d)
            .message("Cool!")
            .entryDate(Instant.now());
        Vote vote2 = new Vote()
            .userKey("uk2")
            .value(5d)
            .message("Cool!")
            .entryDate(Instant.now());
        Vote vote3 = new Vote()
            .userKey("uk3")
            .value(5d)
            .message("Cool!")
            .entryDate(Instant.now());
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();
        vote1.setXmEntity(xmEntity);
        vote2.setXmEntity(xmEntity);
        vote3.setXmEntity(xmEntity);

        Rating rating1 = RatingResourceIntTest.createEntity(em);
        Rating rating2 = RatingResourceIntTest.createEntity(em);
        em.persist(rating1);
        em.persist(rating2);
        em.flush();
        vote1.setRating(rating1);
        vote2.setRating(rating1);
        vote3.setRating(rating2);
        voteRepository.save(vote1);
        voteRepository.save(vote2);
        voteRepository.save(vote3);

        assertThat(voteRepository.countByRatingId(rating1.getId())).isEqualTo(2);

        restRatingMockMvc.perform(get("/api/ratings/{id}/votes/count", rating1.getId()))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.count").value(2));
        ;
    }

}
