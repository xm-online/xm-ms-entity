package com.icthh.xm.ms.entity.config.elasticsearch;

import static java.lang.System.currentTimeMillis;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexServiceUnitTest;
import com.icthh.xm.ms.entity.service.tenant.provisioner.TenantElasticsearchProvisionerUnitTest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class IndexReinitializer {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @PostConstruct
    public void resetIndex() {
        long t = currentTimeMillis();
        elasticsearchOperations.deleteIndex(TenantElasticsearchProvisionerUnitTest.INDEX_NAME);
        elasticsearchOperations.deleteIndex(ElasticsearchIndexServiceUnitTest.INDEX_KEY);
        t = currentTimeMillis() - t;
        logger.info("All elasticsearch indexes reset in {} ms", t);
    }
}
