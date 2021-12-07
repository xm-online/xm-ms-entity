package com.icthh.xm.ms.entity;

import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class BookElasticsearchContainer extends ElasticsearchContainer {


    private static final String CLUSTER_NAME = "cluster.name";

    private static final String ELASTIC_SEARCH = "elasticsearch";

    public BookElasticsearchContainer() {
        this.addFixedExposedPort(9200, 9200);
        this.addFixedExposedPort(9300, 9300);
        this.addEnv(CLUSTER_NAME, ELASTIC_SEARCH);
    }
}
