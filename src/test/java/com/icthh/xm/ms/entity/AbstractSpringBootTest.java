package com.icthh.xm.ms.entity;

import com.icthh.xm.ms.entity.config.LepConfiguration;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * Abstract test for extension for any SpringBoot test.
 *
 * This class prevents Spring test context refreshing between test runs as in case when each Test defines own
 * @SpringBootTest configuration. Marks test with junit @Category
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
    EntityApp.class,
    SecurityBeanOverrideConfiguration.class,
    WebappTenantOverrideConfiguration.class,
    LepConfiguration.class
})
@Category(AbstractSpringBootTest.class)
@Slf4j
public abstract class AbstractSpringBootTest {

    @ClassRule
    public static ElasticsearchContainer elasticsearchContainer = new XmTestElasticsearchContainer();


    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /**
     * Clean data from elastic by delete all query without index deletion due to performance reasons.
     */
    protected void cleanElasticsearch() {

        StopWatch stopWatch = StopWatch.createStarted();

        DeleteQuery deleteQuery = new DeleteQuery();
        deleteQuery.setQuery(QueryBuilders.matchAllQuery());
        elasticsearchTemplate.delete(deleteQuery, XmEntity.class);

        log.info("Elasticsearch index for XmEntity cleaned in {} ms", stopWatch.getTime());

    }

    @BeforeAll
    public static void startElastic() {
        elasticsearchContainer.start();
    }

    @AfterAll
    public static void stopElastic() {
        elasticsearchContainer.stop();
    }

    @BeforeEach
    public void beforeElastic() {
        initElasticsearch();
    }

    /**
     * Recreates Index in elasticsearch.
     *
     * Should be run after tenantContextHolder already has a tenant.
     *
     * It is preferable to run only once per Test class to minimise index movement operations.
     */
    protected void initElasticsearch() {

        StopWatch stopWatch = StopWatch.createStarted();
        if (elasticsearchTemplate.indexExists(XmEntity.class)) {
            elasticsearchTemplate.deleteIndex(XmEntity.class);
        }
        elasticsearchTemplate.createIndex(XmEntity.class);
        elasticsearchTemplate.putMapping(XmEntity.class);

        log.info("Elasticsearch index for XmEntity initialized in {} ms", stopWatch.getTime());

    }

}
