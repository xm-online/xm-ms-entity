package com.icthh.xm.ms.entity.repository;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

@Slf4j
@ContextConfiguration(initializers = {JsonPostgresConfigIntTest.Initializer.class})
@ActiveProfiles("pg-test")
public class JsonPostgresConfigIntTest extends AbstractSpringBootTest {

    private static final String FIRST_DATA_KEY = "firstDataKey";
    private static final String FIRST_DATA_VALUE = "firstDataValue";
    private static final String SECOND_DATA_VALUE = "secondDataValue";

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:12.7")
        .withDatabaseName("entity")
        .withUsername("sa")
        .withPassword("sa");

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
                "spring.datasource.username=" + postgreSQLContainer.getUsername(),
                "spring.datasource.password=" + postgreSQLContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
            log.info("spring.datasource.url: {}", postgreSQLContainer.getJdbcUrl());
        }
    }

    @Autowired
    private XmEntityRepository entityRepository;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private LepManager lepManager;

    @Autowired
    private XmAuthenticationContextHolder xmAuthenticationContextHolder;

    @Before
    public void beforeTransaction() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @Test
    public void searchXmEntityByJsonbDataTest() {
        XmEntity firstEntity = createEntity(Map.of(FIRST_DATA_KEY, FIRST_DATA_VALUE));
        XmEntity secondEntity = createEntity(Map.of(FIRST_DATA_KEY, SECOND_DATA_VALUE));
        XmEntity thirdEntity = createEntity(Map.of());
        entityRepository.saveAll(List.of(firstEntity, secondEntity, thirdEntity));

        List<XmEntity> entities = entityRepository.findAll(Specification.where((root, query, cb) ->
                cb.equal(
                    cb.function(
                        "jsonb_extract_path_text",
                        String.class,
                        root.get(XmEntity_.DATA),
                        cb.literal(FIRST_DATA_KEY)),
                    FIRST_DATA_VALUE)
            )
        );
        assertEquals(entities.size(), 1);
        assertEquals(entities.get(0).getData().get(FIRST_DATA_KEY), FIRST_DATA_VALUE);
    }

    private XmEntity createEntity(Map<String, Object> data) {
        return new XmEntity()
            .typeKey("TYPE1")
            .key(randomUUID())
            .name("name")
            .startDate(now())
            .updateDate(now())
            .data(data);
    }

}
