package com.icthh.xm.ms.entity.config.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;

//@TestConfiguration
//@EnableConfigurationProperties(ElasticsearchProperties.class)
@RequiredArgsConstructor
@Slf4j
public class EmbeddedElasticsearchConfig {

    private final ElasticsearchProperties elasticsearchProperties;

    // FIXME: After investigation it was realized, that org.elasticsearch.client.Client instance was cached somewhere.
    //  Even after another Client instance was created in new Spring context the old one is still used for queries.
    //  However after creating new instance the old one seems to be broken and should be discarded.
    //  Proposed solution is to create pure Client singleton instance which is shared between Spring contexts.
    private static Client client = null;

    // TODO: 22-Jan-19 Consider to use testconteiners instead of embedded ES
    //  (Embedded ES not supported anymore, see
    //  https://www.elastic.co/blog/elasticsearch-the-server)
    @Bean
    @Primary
    public Client elasticsearchClient() throws Exception {
        if (client == null) {
            client = initClient();
        }
        return client;
    }


    private Client initClient() throws Exception {


        log.info("Init embedded elasticsearchClient for TEST");
        NodeClientFactoryBean factoryBean = new NodeClientFactoryBean(true);
        String pathHome = elasticsearchProperties.getProperties()
                                                 .getOrDefault("path.home", "build/elasticsearch");
        factoryBean.setPathHome(pathHome);
        String pathData = elasticsearchProperties.getProperties()
                                                 .getOrDefault("path.data", pathHome + "/data");
        factoryBean.setPathData(pathData);
        factoryBean.afterPropertiesSet();

        return factoryBean.getObject();
    }
}
