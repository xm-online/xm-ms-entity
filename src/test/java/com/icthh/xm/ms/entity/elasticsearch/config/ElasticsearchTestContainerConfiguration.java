package com.icthh.xm.ms.entity.elasticsearch.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static com.icthh.xm.ms.entity.elasticsearch.config.ElasticsearchTestContainer.ELASTICSEARCH_PORT;
import static com.icthh.xm.ms.entity.elasticsearch.config.ElasticsearchTestContainer.ELASTICSEARCH_SCHEME;

@Slf4j
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
        HttpHost httpHost = new HttpHost(elasticTestContainer.getHost(), ELASTICSEARCH_PORT, ELASTICSEARCH_SCHEME);
        RestClientBuilder builder = RestClient.builder(httpHost);
        return builder.build();
    }
}
