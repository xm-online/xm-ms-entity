package com.icthh.xm.ms.entity;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.TestLepUpdateModeConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import static java.nio.charset.StandardCharsets.UTF_8;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

/**
 * Abstract test for extension for any SpringBoot test.
 *
 * This class prevents Spring test context refreshing between test runs as in case when each Test defines own
 * @SpringBootTest configuration. Marks test with junit @Category
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    TestLepUpdateModeConfiguration.class,
    LepConfiguration.class,
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class
})
@Category(AbstractSpringBootTest.class)
@Slf4j
public abstract class AbstractSpringBootTest {

    @Autowired
    private ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

    /**
     * Clean data from elastic by delete all query without index deletion due to performance reasons.
     */
    protected void cleanElasticsearch(TenantContextHolder tenantContextHolder) {

        StopWatch stopWatch = StopWatch.createStarted();

        String tenantName = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toLowerCase();
        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantName);
        elasticsearchTemplateWrapper.deleteIndex(indexName);
        elasticsearchTemplateWrapper.createIndex(indexName);

        elasticsearchTemplateWrapper.putMapping(indexName, ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, getDefaultMapping());

        log.info("Elasticsearch index for XmEntity cleaned in {} ms", stopWatch.getTime());

    }

    /**
     * Recreates Index in elasticsearch.
     *
     * Should be run after tenantContextHolder already has a tenant.
     *
     * It is preferable to run only once per Test class to minimise index movement operations.
     */
    protected void initElasticsearch(TenantContextHolder tenantContextHolder) {

        StopWatch stopWatch = StopWatch.createStarted();

        String tenantName = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toLowerCase();
        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantName);
        elasticsearchTemplateWrapper.deleteIndex(indexName);
        elasticsearchTemplateWrapper.createIndex(indexName);

        elasticsearchTemplateWrapper.putMapping(indexName, ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, getDefaultMapping());

        log.info("Elasticsearch index for XmEntity initialized in {} ms", stopWatch.getTime());

    }

    private String getDefaultMapping() {
        String location = "/config/elastic/test-mapping.json";
        try {
            return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
