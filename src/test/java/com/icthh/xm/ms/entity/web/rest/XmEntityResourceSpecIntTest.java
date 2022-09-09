package com.icthh.xm.ms.entity.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
import com.icthh.xm.ms.entity.repository.EventRepository;
import com.icthh.xm.ms.entity.repository.SpringXmEntityRepository;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityPermittedRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.repository.search.XmEntityPermittedSearchRepository;
import com.icthh.xm.ms.entity.service.AttachmentService;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.JsonValidationService;
import com.icthh.xm.ms.entity.service.LifecycleLepStrategyFactory;
import com.icthh.xm.ms.entity.service.LinkService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.SimpleTemplateProcessor;
import com.icthh.xm.ms.entity.service.StorageService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.XmEntityTemplatesSpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.ms.entity.config.TenantConfigMockConfiguration.getXmEntityTemplatesSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@WithMockUser(authorities = {"SUPER-ADMIN"})
public class XmEntityResourceSpecIntTest extends AbstractSpringBootTest {

    private static final String DEFAULT_TYPE_KEY = "DEMO.TEST";
    private static final String DEFAULT_KEY = "KEY";
    private static final String DEFAULT_NAME = "Test";
    private static final String NEW_STATE = "NEW";

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private XmEntityRepositoryInternal xmEntityRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SpringXmEntityRepository springXmEntityRepository;

    private XmEntityServiceImpl xmEntityServiceImpl;

    @Autowired
    private FunctionService functionService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileEventProducer profileEventProducer;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    XmEntitySpecService xmEntitySpecService;

    @Autowired
    XmEntityTemplatesSpecService xmEntityTemplatesSpecService;

    @Autowired
    LifecycleLepStrategyFactory lifeCycleService;

    @Autowired
    XmEntityPermittedRepository xmEntityPermittedRepository;

    @Autowired
    LinkService linkService;

    @Autowired
    StorageService storageService;

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    XmEntityPermittedSearchRepository xmEntityPermittedSearchRepository;

    @Autowired
    XmEntityTenantConfigService tenantConfigService;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @Spy
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;

    private MockMvc restXmEntityMockMvc;

    @Autowired
    private Validator validator;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private JsonValidationService validationService;

    @BeforeTransaction
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "SPECIFICATIONS");
    }

    @SneakyThrows
    @Before
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, "SPECIFICATIONS");

        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));

        String tenantName = getRequiredTenantKeyValue(tenantContextHolder);
        String config = getXmEntityTemplatesSpec(tenantName);
        String key = applicationProperties.getSpecificationTemplatesPathPattern().replace("{tenantName}", tenantName);
        xmEntityTemplatesSpecService.onRefresh(key, config);

        XmEntityServiceImpl xmEntityServiceImpl = new XmEntityServiceImpl(xmEntitySpecService,
            xmEntityTemplatesSpecService,
            xmEntityRepository,
            lifeCycleService,
            xmEntityPermittedRepository,
            profileService,
            linkService,
            storageService,
            attachmentService,
            xmEntityPermittedSearchRepository,
            startUpdateDateGenerationStrategy,
            authContextHolder,
            objectMapper,
            mock(UniqueFieldRepository.class),
            springXmEntityRepository,
            new TypeKeyWithExtends(tenantConfigService),
            new SimpleTemplateProcessor(objectMapper),
            eventRepository,
            validationService);

        xmEntityServiceImpl.setSelf(xmEntityServiceImpl);

        this.xmEntityServiceImpl = xmEntityServiceImpl;

        XmEntityResource resourceMock = mock(XmEntityResource.class);
        when(resourceMock.createXmEntity(any())).thenReturn(ResponseEntity.created(new URI("")).build());
        XmEntityResource xmEntityResourceMock = new XmEntityResource(xmEntityServiceImpl,
            profileService,
            profileEventProducer,
            functionService,
            tenantService,
            resourceMock
        );
        this.restXmEntityMockMvc = MockMvcBuilders.standaloneSetup(xmEntityResourceMock)
            .setValidator(validator)
            .build();
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    public static XmEntity createEntityByData(Map data) {
        return new XmEntity()
            .key(DEFAULT_KEY)
            .typeKey(DEFAULT_TYPE_KEY)
            .stateKey(NEW_STATE)
            .name(DEFAULT_NAME)
            .data(data);
    }

    @Test
    @Transactional
    public void testCreateXmEntity() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();
        Map expectedData = Map.of("tenantKey", "tenantCreated");
        XmEntity xmEntity = createEntityByData(expectedData);

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().isCreated());

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate + 1);
        XmEntity testXmEntity = xmEntityList.get(xmEntityList.size() - 1);
        assertThat(testXmEntity.getKey()).isEqualTo(DEFAULT_KEY);
        assertThat(testXmEntity.getTypeKey()).isEqualTo(DEFAULT_TYPE_KEY);
        assertThat(testXmEntity.getStateKey()).isEqualTo(NEW_STATE);
        assertThat(testXmEntity.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(testXmEntity.getData()).isEqualTo(expectedData);
    }

    @Test
    @Transactional
    public void testDoesNotCreateXmEntityIfJsonNotValid() throws Exception {
        int databaseSizeBeforeCreate = xmEntityRepository.findAll().size();
        XmEntity xmEntity = createEntityByData(Map.of("tenantKey", 123));

        restXmEntityMockMvc.perform(post("/api/xm-entities")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(xmEntity)))
            .andExpect(status().is4xxClientError());

        List<XmEntity> xmEntityList = xmEntityRepository.findAll();
        assertThat(xmEntityList).hasSize(databaseSizeBeforeCreate);
    }
}
