package com.icthh.xm.ms.entity.config.elasticsearch;

import static java.lang.System.currentTimeMillis;

import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
public class IndexReinitializer {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

    @PostConstruct
    public void resetIndex() {
        long t = currentTimeMillis();
        elasticsearchTemplateWrapper.deleteAllIndexes();
        t = currentTimeMillis() - t;
        logger.info("All elasticsearch indexes reset in {} ms", t);
    }
}
