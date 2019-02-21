package com.icthh.xm.ms.entity;

import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.elasticsearch.EmbeddedElasticsearchConfig;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * Abstract test for extension for any SpringBoot test.
 * This class prevents Spring test context refreshing between test runs as in case when
 * each Test defines own @SpringBootTest configuration.
 * Marks test with junit @Category
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class,
    EmbeddedElasticsearchConfig.class
})
@Category(AbstractSpringBootTest.class)
public abstract class AbstractSpringBootTest {
}
