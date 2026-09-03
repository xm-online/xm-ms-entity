package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.TagRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.TagService;
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

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.icthh.xm.ms.entity.web.rest.facade.TagFacade;
import com.icthh.xm.ms.entity.service.mapper.TagMapper;

/**
 * Test class for the TagResource REST controller.
 *
 * @see TagResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class TagResourceIntTest extends AbstractJupiterSpringBootTest {

    private static final String DEFAULT_TYPE_KEY = "TEST";
    private static final String UPDATED_TYPE_KEY = "FAVORIT";

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private TagResource tagResource;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JsonMapper jsonMapper;

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
    private PermittedRepository permittedRepository;

    @Autowired
    private XmEntityRepository xmEntityRepository;

    @Autowired
    private TagMapper tagMapper;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private MockMvc restTagMockMvc;

    private Tag tag;

    private TagService tagService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(startUpdateDateGenerationStrategy.generateStartDate()).thenReturn(DEFAULT_START_DATE);

        tagService = new TagService(
            tagRepository,
            permittedRepository,
            startUpdateDateGenerationStrategy,
            xmEntityRepository);

        TagFacade tagFacade = new TagFacade(tagService, tagMapper);
        TagResource tagResourceMock = new TagResource(tagResource, tagFacade);
        this.restTagMockMvc = MockMvcBuilders.standaloneSetup(tagResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper)).build();

        tag = createEntity(em);

    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     * <p>
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Tag createEntity(EntityManager em) {
        Tag tag = new Tag()
            .typeKey(DEFAULT_TYPE_KEY)
            .name(DEFAULT_NAME)
            .startDate(DEFAULT_START_DATE);
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();
        tag.setXmEntity(xmEntity);
        return tag;
    }

    @BeforeEach
    public void initTest() {
        //   tagSearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void createTag() throws Exception {
        int databaseSizeBeforeCreate = tagRepository.findAll().size();

        // Create the Tag
        restTagMockMvc.perform(post("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(tag, tagMapper.toDto(tag))))
                .andDo(print())
            .andExpect(status().isCreated());

        // Validate the Tag in the database
        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeCreate + 1);
        Tag testTag = tagList.get(tagList.size() - 1);
        assertThat(testTag.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testTag.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testTag.getStartDate()).isEqualTo(DEFAULT_START_DATE);
    }

    @Test
    @Transactional
    public void createTagWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = tagRepository.findAll().size();

        // Create the Tag with an existing ID
        tag.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restTagMockMvc.perform(post("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(tag, tagMapper.toDto(tag))))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()));

        // Validate the Alice in the database
        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = tagRepository.findAll().size();
        // set the field null
        tag.setTypeKey(null);

        // Create the Tag, which fails.

        restTagMockMvc.perform(post("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(tag, tagMapper.toDto(tag))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("tag"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("typeKey"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @Disabled("see TagResourceExtendedIntTest.checkStartDateIsNotRequired instead")
    public void checkStartDateIsRequired() throws Exception {
        int databaseSizeBeforeTest = tagRepository.findAll().size();
        // set the field null
        tag.setStartDate(null);

        // Create the Tag, which fails.

        restTagMockMvc.perform(post("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(tag, tagMapper.toDto(tag))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("tag"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("startDate"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllTags() throws Exception {
        // Initialize the database
        tagRepository.saveAndFlush(tag);

        // Get all the tagList
        restTagMockMvc.perform(get("/api/tags?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(tag.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].startDate").value(hasItem(DEFAULT_START_DATE.toString())));
    }

    @Test
    @Transactional
    public void getTagsByXmEntity() throws Exception {
        XmEntity entity1 = XmEntityResourceIntTest.createEntity();
        em.persist(entity1);
        XmEntity entity2 = XmEntityResourceIntTest.createEntity();
        em.persist(entity2);
        em.flush();

        // wrong entity, right typeKey - must NOT be returned
        Tag wrongEntity = tagRepository.saveAndFlush(
            new Tag().typeKey("TEST").name("wrong-entity").startDate(DEFAULT_START_DATE).xmEntity(entity1));
        // right entity, wrong typeKey - must NOT be returned
        Tag wrongTypeKey = tagRepository.saveAndFlush(
            new Tag().typeKey("FAVORIT").name("wrong-typeKey").startDate(DEFAULT_START_DATE).xmEntity(entity2));
        // right entity, right typeKey - the actual matches (3, to span 2 pages of size 2)
        Tag match1 = tagRepository.saveAndFlush(new Tag().typeKey("TEST").name("match-1").startDate(DEFAULT_START_DATE).xmEntity(entity2));
        Tag match2 = tagRepository.saveAndFlush(new Tag().typeKey("TEST").name("match-2").startDate(DEFAULT_START_DATE).xmEntity(entity2));
        Tag match3 = tagRepository.saveAndFlush(new Tag().typeKey("TEST").name("match-3").startDate(DEFAULT_START_DATE).xmEntity(entity2));

        // first page: only matches, none of the excluded ones
        restTagMockMvc.perform(get("/api/xm-entities/" + entity2.getId() + "/tags/TEST?sort=id,asc&page=0&size=2"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.[*].id").value(hasItems(match1.getId().intValue(), match2.getId().intValue())))
            .andExpect(jsonPath("$.[*].id").value(not(hasItem(wrongEntity.getId().intValue()))))
            .andExpect(jsonPath("$.[*].id").value(not(hasItem(wrongTypeKey.getId().intValue()))));

        // second page: the third match, proving pagination actually advances
        restTagMockMvc.perform(get("/api/xm-entities/" + entity2.getId() + "/tags/TEST?sort=id,asc&page=1&size=2"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$.[0].id").value(match3.getId().intValue()));
    }

    @Test
    @Transactional
    public void getTag() throws Exception {
        // Initialize the database
        tagRepository.saveAndFlush(tag);

        // Get the tag
        restTagMockMvc.perform(get("/api/tags/{id}", tag.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(tag.getId().intValue()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.startDate").value(DEFAULT_START_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingTag() throws Exception {
        // Get the tag
        restTagMockMvc.perform(get("/api/tags/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateTag() throws Exception {
        // Initialize the database
        tagRepository.saveAndFlush(tag);
        int databaseSizeBeforeUpdate = tagRepository.findAll().size();

        // Update the tag
        Tag updatedTag = tagRepository.findById(tag.getId())
            .orElseThrow(NullPointerException::new);
        updatedTag
            .typeKey(UPDATED_TYPE_KEY)
            .name(UPDATED_NAME)
            .startDate(UPDATED_START_DATE);

        restTagMockMvc.perform(put("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(updatedTag, tagMapper.toDto(updatedTag))))
            .andExpect(status().isOk());

        // Validate the Tag in the database
        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeUpdate);
        Tag testTag = tagList.get(tagList.size() - 1);
        assertThat(testTag.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testTag.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testTag.getStartDate()).isEqualTo(UPDATED_START_DATE);
    }

    @Test
    @Transactional
    public void updateNonExistingTag() throws Exception {
        int databaseSizeBeforeUpdate = tagRepository.findAll().size();

        // Create the Tag

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restTagMockMvc.perform(put("/api/tags")
                                   .contentType(TestUtil.APPLICATION_JSON_UTF8)
                                   .content(TestUtil.assertObjectsAndConvertToJsonBytesDto(tag, tagMapper.toDto(tag))))
            .andExpect(status().isCreated());

        // Validate the Tag in the database
        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteTag() throws Exception {
        // Initialize the database
        tagRepository.saveAndFlush(tag);
        int databaseSizeBeforeDelete = tagRepository.findAll().size();

        // Get the tag
        restTagMockMvc.perform(delete("/api/tags/{id}", tag.getId())
                                   .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Tag> tagList = tagRepository.findAll();
        assertThat(tagList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Tag.class);
        Tag tag1 = new Tag();
        tag1.setId(1L);
        Tag tag2 = new Tag();
        tag2.setId(tag1.getId());
        assertThat(tag1).isEqualTo(tag2);
        tag2.setId(2L);
        assertThat(tag1).isNotEqualTo(tag2);
        tag1.setId(null);
        assertThat(tag1).isNotEqualTo(tag2);
    }
}
