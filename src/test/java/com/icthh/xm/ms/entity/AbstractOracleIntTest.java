package com.icthh.xm.ms.entity;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.OracleTestContainer;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.BeforeTransaction;

/**
 * Base class for integration tests that must run on a real Oracle: json value extraction and ordering
 * behave differently there than on PostgreSQL.
 *
 * <p>The schema comes from Hibernate ({@code ddl-auto: create-drop}), not from Liquibase: these tests cover
 * query semantics, and Hibernate then guarantees that column types match what the mapping expects.
 */
@ActiveProfiles("oracle-test")
public abstract class AbstractOracleIntTest extends AbstractJupiterSpringBootTest {

    public static final String TENANT = "TEST";

    @DynamicPropertySource
    static void oracleProperties(DynamicPropertyRegistry registry) {
        var container = OracleTestContainer.getInstance();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @Autowired
    protected TenantContextHolder tenantContextHolder;
    @Autowired
    protected LepManager lepManager;
    @Autowired
    protected XmAuthenticationContextHolder xmAuthenticationContextHolder;
    @Autowired
    protected XmEntitySpecService xmEntitySpecService;
    @Autowired
    protected ApplicationProperties applicationProperties;

    @BeforeEach
    @BeforeTransaction
    public void setUpTenantContext() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT);
        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthenticationContextHolder.getContext());
        });
    }

    @AfterEach
    public void tearDownTenantContext() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        lepManager.endThreadContext();
    }

    @SneakyThrows
    protected void pushDbSearchSpec() {
        String yml = IOUtils.toString(
            new ClassPathResource("config/specs/xmentityspec-dbsearch.yml").getInputStream(), UTF_8);
        String key = applicationProperties.getSpecificationFolderPathPattern()
            .replace("{tenantName}", TENANT)
            .replace("*.yml", "dbsearch.yml");
        xmEntitySpecService.onRefresh(key, yml);
        xmEntitySpecService.refreshFinished(List.of(key));
    }

    protected static XmEntity newEntity(String typeKey, String name, Map<String, Object> data) {
        return new XmEntity()
            .typeKey(typeKey)
            .key(UUID.randomUUID().toString())
            .name(name)
            .startDate(Instant.now())
            .updateDate(Instant.now())
            .data(data);
    }
}
