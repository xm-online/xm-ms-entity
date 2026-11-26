package com.icthh.xm.ms.entity;

import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.TestLepUpdateModeConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.security.access.XmEntityDynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntityFunctionExecutorService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.web.rest.facade.LinkFacade;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Abstract test for extension for any SpringBoot test.
 * This class prevents Spring test context refreshing between test runs as in case when each Test defines own
 * @SpringBootTest configuration. Marks test with junit @Category
 *
 * <p>Every {@code @MockitoBean}, {@code @MockitoSpyBean} or {@code @ActiveProfiles} declared on a subclass changes the
 * Spring context cache key and starts one more full application context. Beans that a test needs to stub are spied
 * here once for all subclasses: an unstubbed spy runs the real method, and stubs are reset after each test.
 * Stub a spy with {@code doReturn/doAnswer(...).when(spy)}, so the real method is not called while stubbing.
 */
@Slf4j
@SpringBootTest(classes = {
    TestLepUpdateModeConfiguration.class,
    LepConfiguration.class,
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class
})
@MockitoSpyBean(types = {
    XmEntityDynamicPermissionCheckService.class,
    LinkFacade.class,
    XmEntitySpecService.class,
    XmEntityTenantConfigService.class,
    StartUpdateDateGenerationStrategy.class,
    XmEntityFunctionExecutorService.class,
    XmEntitySearchRepository.class
})
@Tag("com.icthh.xm.ms.entity.AbstractSpringBootTest")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractJupiterSpringBootTest {

}
