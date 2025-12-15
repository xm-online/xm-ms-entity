package com.icthh.xm.ms.entity;

import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.TestLepUpdateModeConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Abstract test for extension for any SpringBoot test.
 * This class prevents Spring test context refreshing between test runs as in case when each Test defines own
 * @SpringBootTest configuration. Marks test with junit @Category
 */
@Slf4j
@SpringBootTest(classes = {
    TestLepUpdateModeConfiguration.class,
    LepConfiguration.class,
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class
})
@Tag("com.icthh.xm.ms.entity.AbstractSpringBootTest")
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractJupiterSpringBootTest {

}
