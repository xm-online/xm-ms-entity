package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import com.icthh.xm.commons.errors.ExceptionTranslator;
import com.icthh.xm.ms.entity.EntityApp;

import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;

import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Rating;
import com.icthh.xm.ms.entity.domain.Vote;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.VoteRepository;
import com.icthh.xm.ms.entity.repository.search.VoteSearchRepository;

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

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the VoteResource REST controller.
 *
 * @see VoteResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class VoteResourceIntTest {

    private static final String DEFAULT_USER_KEY = "AAAAAAAAAA";
    private static final String UPDATED_USER_KEY = "BBBBBBBBBB";

    private static final Double DEFAULT_VALUE = 1D;
    private static final Double UPDATED_VALUE = 2D;

    private static final String DEFAULT_MESSAGE = "AAAAAAAAAA";
    private static final String UPDATED_MESSAGE = "BBBBBBBBBB";

    private static final Instant DEFAULT_ENTRY_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_ENTRY_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private VoteSearchRepository voteSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restVoteMockMvc;

    private Vote vote;

    @Before
    public void setup() {

        TenantContext.setCurrent("RESINTTEST");

        MockitoAnnotations.initMocks(this);
        VoteResource voteResource = new VoteResource(voteRepository, voteSearchRepository);
        this.restVoteMockMvc = MockMvcBuilders.standaloneSetup(voteResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();

        vote = createEntity(em);

    }

    @After
    public void finalize() {
        TenantContext.setCurrent("XM");
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Vote createEntity(EntityManager em) {
        Vote vote = new Vote()
            .userKey(DEFAULT_USER_KEY)
            .value(DEFAULT_VALUE)
            .message(DEFAULT_MESSAGE)
            .entryDate(DEFAULT_ENTRY_DATE);
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity(em);
        em.persist(xmEntity);
        em.flush();
        vote.setXmEntity(xmEntity);

        Rating rating = RatingResourceIntTest.createEntity(em);
        em.persist(rating);
        em.flush();
        vote.setRating(rating);
        return vote;
    }

    @Before
    public void initTest() {
    //    voteSearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void createVote() throws Exception {
        int databaseSizeBeforeCreate = voteRepository.findAll().size();

        // Create the Vote
        restVoteMockMvc.perform(post("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(vote)))
            .andExpect(status().isCreated());

        // Validate the Vote in the database
        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate + 1);
        Vote testVote = voteList.get(voteList.size() - 1);
        assertThat(testVote.getUserKey()).isEqualTo(DEFAULT_USER_KEY);
        assertThat(testVote.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testVote.getMessage()).isEqualTo(DEFAULT_MESSAGE);
        assertThat(testVote.getEntryDate()).isEqualTo(DEFAULT_ENTRY_DATE);

        // Validate the Vote in Elasticsearch
        Vote voteEs = voteSearchRepository.findOne(testVote.getId());
        assertThat(voteEs).isEqualToComparingFieldByField(testVote);
    }

    @Test
    @Transactional
    public void createVoteWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = voteRepository.findAll().size();

        // Create the Vote with an existing ID
        vote.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restVoteMockMvc.perform(post("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(vote)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkUserKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = voteRepository.findAll().size();
        // set the field null
        vote.setUserKey(null);

        // Create the Vote, which fails.

        restVoteMockMvc.perform(post("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(vote)))
            .andExpect(status().isBadRequest());

        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = voteRepository.findAll().size();
        // set the field null
        vote.setValue(null);

        // Create the Vote, which fails.

        restVoteMockMvc.perform(post("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(vote)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("vote"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("value"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkEntryDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = voteRepository.findAll().size();
        // set the field null
        vote.setEntryDate(null);

        // Create the Vote, which fails.

        restVoteMockMvc.perform(post("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(vote)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("vote"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("entryDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllVotes() throws Exception {
        // Initialize the database
        voteRepository.saveAndFlush(vote);

        // Get all the voteList
        restVoteMockMvc.perform(get("/api/votes?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(vote.getId().intValue())))
            .andExpect(jsonPath("$.[*].userKey").value(hasItem(DEFAULT_USER_KEY)))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE)))
            .andExpect(jsonPath("$.[*].message").value(hasItem(DEFAULT_MESSAGE)))
            .andExpect(jsonPath("$.[*].entryDate").value(hasItem(DEFAULT_ENTRY_DATE.toString())));
    }

    @Test
    @Transactional
    public void getVote() throws Exception {
        // Initialize the database
        voteRepository.saveAndFlush(vote);

        // Get the vote
        restVoteMockMvc.perform(get("/api/votes/{id}", vote.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(vote.getId().intValue()))
            .andExpect(jsonPath("$.userKey").value(DEFAULT_USER_KEY))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE))
            .andExpect(jsonPath("$.message").value(DEFAULT_MESSAGE))
            .andExpect(jsonPath("$.entryDate").value(DEFAULT_ENTRY_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingVote() throws Exception {
        // Get the vote
        restVoteMockMvc.perform(get("/api/votes/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateVote() throws Exception {
        // Initialize the database
        voteRepository.saveAndFlush(vote);
        voteSearchRepository.save(vote);
        int databaseSizeBeforeUpdate = voteRepository.findAll().size();

        // Update the vote
        Vote updatedVote = voteRepository.findOne(vote.getId());
        updatedVote
            .userKey(UPDATED_USER_KEY)
            .value(UPDATED_VALUE)
            .message(UPDATED_MESSAGE)
            .entryDate(UPDATED_ENTRY_DATE);

        restVoteMockMvc.perform(put("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedVote)))
            .andExpect(status().isOk());

        // Validate the Vote in the database
        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeUpdate);
        Vote testVote = voteList.get(voteList.size() - 1);
        assertThat(testVote.getUserKey()).isEqualTo(UPDATED_USER_KEY);
        assertThat(testVote.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testVote.getMessage()).isEqualTo(UPDATED_MESSAGE);
        assertThat(testVote.getEntryDate()).isEqualTo(UPDATED_ENTRY_DATE);

        // Validate the Vote in Elasticsearch
        Vote voteEs = voteSearchRepository.findOne(testVote.getId());
        assertThat(voteEs).isEqualToComparingFieldByField(testVote);
    }

    @Test
    @Transactional
    public void updateNonExistingVote() throws Exception {
        int databaseSizeBeforeUpdate = voteRepository.findAll().size();

        // Create the Vote

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restVoteMockMvc.perform(put("/api/votes")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(vote)))
            .andExpect(status().isCreated());

        // Validate the Vote in the database
        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteVote() throws Exception {
        // Initialize the database
        voteRepository.saveAndFlush(vote);
        voteSearchRepository.save(vote);
        int databaseSizeBeforeDelete = voteRepository.findAll().size();

        // Get the vote
        restVoteMockMvc.perform(delete("/api/votes/{id}", vote.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate Elasticsearch is empty
        boolean voteExistsInEs = voteSearchRepository.exists(vote.getId());
        assertThat(voteExistsInEs).isFalse();

        // Validate the database is empty
        List<Vote> voteList = voteRepository.findAll();
        assertThat(voteList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchVote() throws Exception {
        // Initialize the database
        voteRepository.saveAndFlush(vote);
        voteSearchRepository.save(vote);

        // Search the vote
        restVoteMockMvc.perform(get("/api/_search/votes?query=id:" + vote.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(vote.getId().intValue())))
            .andExpect(jsonPath("$.[*].userKey").value(hasItem(DEFAULT_USER_KEY)))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE)))
            .andExpect(jsonPath("$.[*].message").value(hasItem(DEFAULT_MESSAGE)))
            .andExpect(jsonPath("$.[*].entryDate").value(hasItem(DEFAULT_ENTRY_DATE.toString())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Vote.class);
        Vote vote1 = new Vote();
        vote1.setId(1L);
        Vote vote2 = new Vote();
        vote2.setId(vote1.getId());
        assertThat(vote1).isEqualTo(vote2);
        vote2.setId(2L);
        assertThat(vote1).isNotEqualTo(vote2);
        vote1.setId(null);
        assertThat(vote1).isNotEqualTo(vote2);
    }
}
