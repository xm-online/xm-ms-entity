package com.icthh.xm.ms.entity.config.elasticsearch;

import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

public class ElasticsearchTestContainer extends ElasticsearchContainer {

    private static final String ELASTIC_SEARCH_DOCKER = "elasticsearch:8.7.0";
    private static final String DISCOVERY_TYPE = "discovery.type";
    private static final String DISCOVERY_TYPE_SINGLE_NODE = "single-node";
    private static final String XPACK_SECURITY_ENABLED = "xpack.security.enabled";
    private static final String ACTION_DESTRUCTIVE_REQUIRES_NAME = "action.destructive_requires_name";

    public static final int ELASTICSEARCH_PORT = 9200;
    public static final String ELASTICSEARCH_SCHEME = "http";

    public ElasticsearchTestContainer() {
        super(DockerImageName.parse(ELASTIC_SEARCH_DOCKER)
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"));
        addFixedExposedPort(ELASTICSEARCH_PORT, ELASTICSEARCH_PORT);
        addEnv(DISCOVERY_TYPE, DISCOVERY_TYPE_SINGLE_NODE);
        addEnv(XPACK_SECURITY_ENABLED, Boolean.FALSE.toString());

        // see: https://www.elastic.co/guide/en/elasticsearch/reference/7.17/index-management-settings.html#action-destructive-requires-name
        addEnv(ACTION_DESTRUCTIVE_REQUIRES_NAME, Boolean.FALSE.toString());
    }
}
