package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LocationRepository;
import com.icthh.xm.ms.entity.service.LocationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.util.List;
import javax.persistence.EntityManager;

/**
 * Test class for the LocationResource REST controller.
 *
 * @see LocationResource
 */
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class LocationResourceIntTest extends AbstractSpringBootTest {

    private static final String DEFAULT_TYPE_KEY = "AAAAAAAAAA";
    private static final String UPDATED_TYPE_KEY = "BBBBBBBBBB";

    private static final String DEFAULT_COUNTRY_KEY = "AAAAAAAAAA";
    private static final String UPDATED_COUNTRY_KEY = "BBBBBBBBBB";

    private static final Double DEFAULT_LONGITUDE = 1D;
    private static final Double UPDATED_LONGITUDE = 2D;

    private static final Double DEFAULT_LATITUDE = 1D;
    private static final Double UPDATED_LATITUDE = 2D;

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_ADDRESS_LINE_1 = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS_LINE_1 = "BBBBBBBBBB";

    private static final String DEFAULT_ADDRESS_LINE_2 = "AAAAAAAAAA";
    private static final String UPDATED_ADDRESS_LINE_2 = "BBBBBBBBBB";

    private static final String DEFAULT_CITY = "AAAAAAAAAA";
    private static final String UPDATED_CITY = "BBBBBBBBBB";

    private static final String DEFAULT_REGION = "AAAAAAAAAA";
    private static final String UPDATED_REGION = "BBBBBBBBBB";

    private static final String DEFAULT_ZIP = "AAAAAAAAAA";
    private static final String UPDATED_ZIP = "BBBBBBBBBB";

    @Autowired
    private LocationResource locationResource;

    @Autowired
    private LocationRepository locationRepository;

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
    private XmAuthenticationContextHolder authContextHolder;

    private MockMvc restLocationMockMvc;

    private Location location;

    @Autowired
    private Validator validator;

    @Autowired
    private LocationService locationService;

    @Autowired
    private LepManager lepManager;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "RESINTTEST");
    }

    @Before
    public void setup() {

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });

        MockitoAnnotations.initMocks(this);
        LocationResource locationResourceMock = new LocationResource(locationResource, locationService);
        this.restLocationMockMvc = MockMvcBuilders.standaloneSetup(locationResourceMock)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setValidator(validator)
            .setMessageConverters(jacksonMessageConverter).build();

        location = createEntity(em);

    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Location createEntity(EntityManager em) {
        Location location = new Location()
            .typeKey(DEFAULT_TYPE_KEY)
            .countryKey(DEFAULT_COUNTRY_KEY)
            .longitude(DEFAULT_LONGITUDE)
            .latitude(DEFAULT_LATITUDE)
            .name(DEFAULT_NAME)
            .addressLine1(DEFAULT_ADDRESS_LINE_1)
            .addressLine2(DEFAULT_ADDRESS_LINE_2)
            .city(DEFAULT_CITY)
            .region(DEFAULT_REGION)
            .zip(DEFAULT_ZIP);
        // Add required entity
        XmEntity xmEntity = XmEntityResourceIntTest.createEntity();
        em.persist(xmEntity);
        em.flush();
        location.setXmEntity(xmEntity);
        return location;
    }

    @Before
    public void initTest() {
   //     locationSearchRepository.deleteAll();
    }

    @Test
    @Transactional
    public void createLocation() throws Exception {
        int databaseSizeBeforeCreate = locationRepository.findAll().size();

        // Create the Location
        restLocationMockMvc.perform(post("/api/locations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(location)))
            .andExpect(status().isCreated());

        // Validate the Location in the database
        List<Location> locationList = locationRepository.findAll();
        assertThat(locationList).hasSize(databaseSizeBeforeCreate + 1);
        Location testLocation = locationList.get(locationList.size() - 1);
        assertThat(testLocation.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testLocation.getCountryKey()).isEqualTo(DEFAULT_COUNTRY_KEY);
        assertThat(testLocation.getLongitude()).isEqualTo(DEFAULT_LONGITUDE);
        assertThat(testLocation.getLatitude()).isEqualTo(DEFAULT_LATITUDE);
        assertThat(testLocation.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testLocation.getAddressLine1()).isEqualTo(DEFAULT_ADDRESS_LINE_1);
        assertThat(testLocation.getAddressLine2()).isEqualTo(DEFAULT_ADDRESS_LINE_2);
        assertThat(testLocation.getCity()).isEqualTo(DEFAULT_CITY);
        assertThat(testLocation.getRegion()).isEqualTo(DEFAULT_REGION);
        assertThat(testLocation.getZip()).isEqualTo(DEFAULT_ZIP);
    }

    @Test
    @Transactional
    public void createLocationWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = locationRepository.findAll().size();

        // Create the Location with an existing ID
        location.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restLocationMockMvc.perform(post("/api/locations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(location)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.business.idexists"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;

        // Validate the Alice in the database
        List<Location> locationList = locationRepository.findAll();
        assertThat(locationList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkTypeKeyIsRequired() throws Exception {
        int databaseSizeBeforeTest = locationRepository.findAll().size();
        // set the field null
        location.setTypeKey(null);

        // Create the Location, which fails.

        restLocationMockMvc.perform(post("/api/locations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(location)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("error.validation"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
            .andExpect(jsonPath("$.fieldErrors[0].objectName").value("location"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("typeKey"))
            .andExpect(jsonPath("$.fieldErrors[0].message").value("NotNull"))
        ;

        List<Location> locationList = locationRepository.findAll();
        assertThat(locationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void getAllLocations() throws Exception {
        // Initialize the database
        locationRepository.saveAndFlush(location);

        // Get all the locationList
        restLocationMockMvc.perform(get("/api/locations?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(location.getId().intValue())))
            .andExpect(jsonPath("$.[*].typeKey").value(hasItem(DEFAULT_TYPE_KEY.toString())))
            .andExpect(jsonPath("$.[*].countryKey").value(hasItem(DEFAULT_COUNTRY_KEY.toString())))
            .andExpect(jsonPath("$.[*].longitude").value(hasItem(DEFAULT_LONGITUDE.doubleValue())))
            .andExpect(jsonPath("$.[*].latitude").value(hasItem(DEFAULT_LATITUDE.doubleValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
            .andExpect(jsonPath("$.[*].addressLine1").value(hasItem(DEFAULT_ADDRESS_LINE_1.toString())))
            .andExpect(jsonPath("$.[*].addressLine2").value(hasItem(DEFAULT_ADDRESS_LINE_2.toString())))
            .andExpect(jsonPath("$.[*].city").value(hasItem(DEFAULT_CITY.toString())))
            .andExpect(jsonPath("$.[*].region").value(hasItem(DEFAULT_REGION.toString())))
            .andExpect(jsonPath("$.[*].zip").value(hasItem(DEFAULT_ZIP.toString())));
    }

    @Test
    @Transactional
    public void getLocation() throws Exception {
        // Initialize the database
        locationRepository.saveAndFlush(location);

        // Get the location
        restLocationMockMvc.perform(get("/api/locations/{id}", location.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(location.getId().intValue()))
            .andExpect(jsonPath("$.typeKey").value(DEFAULT_TYPE_KEY.toString()))
            .andExpect(jsonPath("$.countryKey").value(DEFAULT_COUNTRY_KEY.toString()))
            .andExpect(jsonPath("$.longitude").value(DEFAULT_LONGITUDE.doubleValue()))
            .andExpect(jsonPath("$.latitude").value(DEFAULT_LATITUDE.doubleValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()))
            .andExpect(jsonPath("$.addressLine1").value(DEFAULT_ADDRESS_LINE_1.toString()))
            .andExpect(jsonPath("$.addressLine2").value(DEFAULT_ADDRESS_LINE_2.toString()))
            .andExpect(jsonPath("$.city").value(DEFAULT_CITY.toString()))
            .andExpect(jsonPath("$.region").value(DEFAULT_REGION.toString()))
            .andExpect(jsonPath("$.zip").value(DEFAULT_ZIP.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingLocation() throws Exception {
        // Get the location
        restLocationMockMvc.perform(get("/api/locations/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("error.notfound"))
            .andExpect(jsonPath("$.error_description").value(notNullValue()))
        ;
    }

    @Test
    @Transactional
    public void updateLocation() throws Exception {
        // Initialize the database
        locationRepository.saveAndFlush(location);
        int databaseSizeBeforeUpdate = locationRepository.findAll().size();

        // Update the location
        Location updatedLocation = locationRepository.findById(location.getId())
            .orElseThrow(NullPointerException::new);
        updatedLocation
            .typeKey(UPDATED_TYPE_KEY)
            .countryKey(UPDATED_COUNTRY_KEY)
            .longitude(UPDATED_LONGITUDE)
            .latitude(UPDATED_LATITUDE)
            .name(UPDATED_NAME)
            .addressLine1(UPDATED_ADDRESS_LINE_1)
            .addressLine2(UPDATED_ADDRESS_LINE_2)
            .city(UPDATED_CITY)
            .region(UPDATED_REGION)
            .zip(UPDATED_ZIP);

        restLocationMockMvc.perform(put("/api/locations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedLocation)))
            .andExpect(status().isOk());

        // Validate the Location in the database
        List<Location> locationList = locationRepository.findAll();
        assertThat(locationList).hasSize(databaseSizeBeforeUpdate);
        Location testLocation = locationList.get(locationList.size() - 1);
        assertThat(testLocation.getTypeKey()).isEqualTo(UPDATED_TYPE_KEY);
        assertThat(testLocation.getCountryKey()).isEqualTo(UPDATED_COUNTRY_KEY);
        assertThat(testLocation.getLongitude()).isEqualTo(UPDATED_LONGITUDE);
        assertThat(testLocation.getLatitude()).isEqualTo(UPDATED_LATITUDE);
        assertThat(testLocation.getName()).isEqualTo(UPDATED_NAME);
        assertThat(testLocation.getAddressLine1()).isEqualTo(UPDATED_ADDRESS_LINE_1);
        assertThat(testLocation.getAddressLine2()).isEqualTo(UPDATED_ADDRESS_LINE_2);
        assertThat(testLocation.getCity()).isEqualTo(UPDATED_CITY);
        assertThat(testLocation.getRegion()).isEqualTo(UPDATED_REGION);
        assertThat(testLocation.getZip()).isEqualTo(UPDATED_ZIP);
    }

    @Test
    @Transactional
    public void updateNonExistingLocation() throws Exception {
        int databaseSizeBeforeUpdate = locationRepository.findAll().size();

        // Create the Location

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restLocationMockMvc.perform(put("/api/locations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(location)))
            .andExpect(status().isCreated());

        // Validate the Location in the database
        List<Location> locationList = locationRepository.findAll();
        assertThat(locationList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteLocation() throws Exception {
        // Initialize the database
        locationRepository.saveAndFlush(location);
        int databaseSizeBeforeDelete = locationRepository.findAll().size();

        // Get the location
        restLocationMockMvc.perform(delete("/api/locations/{id}", location.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Location> locationList = locationRepository.findAll();
        assertThat(locationList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Location.class);
        Location location1 = new Location();
        location1.setId(1L);
        Location location2 = new Location();
        location2.setId(location1.getId());
        assertThat(location1).isEqualTo(location2);
        location2.setId(2L);
        assertThat(location1).isNotEqualTo(location2);
        location1.setId(null);
        assertThat(location1).isNotEqualTo(location2);
    }
}
