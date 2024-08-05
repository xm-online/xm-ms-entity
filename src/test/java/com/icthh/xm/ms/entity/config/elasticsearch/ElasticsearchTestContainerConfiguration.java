package com.icthh.xm.ms.entity.config.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static com.icthh.xm.ms.entity.config.elasticsearch.ElasticsearchTestContainer.ELASTICSEARCH_PORT;

@Slf4j
//@TestConfiguration
public class ElasticsearchTestContainerConfiguration {

    @Bean
    public IndexReinitializer indexReinitializer() {
        return new IndexReinitializer();
    }

    @Bean
    public ElasticsearchContainer elasticTestContainer() {
        ElasticsearchContainer container = new ElasticsearchTestContainer();
        container.start();
        log.info("Elasticsearch test container started");
        return container;
    }

    @Bean
    public RestClient restClient(ElasticsearchContainer elasticTestContainer) {
        HttpHost httpHost = new HttpHost(elasticTestContainer.getHost(), ELASTICSEARCH_PORT, "http");
        RestClientBuilder builder = RestClient.builder(httpHost);
        return builder.build();
    }
}