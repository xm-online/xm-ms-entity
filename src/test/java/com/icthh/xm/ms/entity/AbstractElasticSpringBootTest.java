package com.icthh.xm.ms.entity;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.TestLepUpdateModeConfiguration;
import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchTestContainer;
import com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchTestContainerConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@SpringBootTest(classes = {
    TestLepUpdateModeConfiguration.class,
    LepConfiguration.class,
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class
})
@Category(AbstractSpringBootTest.class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = { ElasticsearchTestContainerConfiguration.class })
public class AbstractElasticSpringBootTest {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ElasticsearchTestContainer elasticsearchContainer;

    @BeforeAll
    public void setUp() {
        elasticsearchContainer.start();
        log.info("Elasticsearch test container started");
    }

    @AfterAll
    public void destroy() {
        elasticsearchContainer.stop();
        log.info("Elasticsearch test container stopped");
    }

    /**
     * Clean data from elastic by delete all query without index deletion due to performance reasons.
     */
    protected void cleanElasticsearch(TenantContextHolder tenantContextHolder) {

        StopWatch stopWatch = StopWatch.createStarted();

        String tenantName = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toLowerCase();
        String indexName = elasticsearchOperations.composeIndexName(tenantName);
        elasticsearchOperations.deleteIndex(indexName);
        elasticsearchOperations.createIndex(indexName);

        elasticsearchOperations.putMapping(indexName, ElasticsearchOperations.INDEX_QUERY_TYPE, getDefaultMapping());

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
        String indexName = elasticsearchOperations.composeIndexName(tenantName);
        elasticsearchOperations.deleteIndex(indexName);
        elasticsearchOperations.createIndex(indexName);

        elasticsearchOperations.putMapping(indexName, ElasticsearchOperations.INDEX_QUERY_TYPE, getDefaultMapping());

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
