package com.icthh.xm.ms.entity.config.elasticsearch;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.client.Client;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;

@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
@RequiredArgsConstructor
public class EmbeddedElasticsearchConfig {

    private final ElasticsearchProperties elasticsearchProperties;

    // TODO: 22-Jan-19 Consider to use testconteiners instead of embedded ES
    //  (Embedded ES not supported anymore, see
    //  https://www.elastic.co/blog/elasticsearch-the-server)
    @Bean
    @Primary
    public Client elasticsearchClient() throws Exception {
        NodeClientFactoryBean factoryBean = new NodeClientFactoryBean(true);
        String pathHome = elasticsearchProperties.getProperties()
            .getOrDefault("path.home", "build/elasticsearch");
        factoryBean.setPathHome(pathHome);
        String pathData = elasticsearchProperties.getProperties()
            .getOrDefault("path.data", pathHome + "/data");
//        factoryBean.setPathConfiguration("config/es.yml");
        factoryBean.setPathData(pathData);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
