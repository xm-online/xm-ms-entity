package com.icthh.xm.ms.entity.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class ElasticsearchConfiguration {

    private final ApplicationProperties.Elastic elastic;

    public ElasticsearchConfiguration(ApplicationProperties applicationProperties) {
        this.elastic = applicationProperties.getElastic();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport elasticsearchTransport) {
        return new ElasticsearchClient(elasticsearchTransport);
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(JacksonJsonpMapper jacksonJsonpMapper) {
        return new RestClientTransport(restClient(), jacksonJsonpMapper);
    }

    @Bean
    public JacksonJsonpMapper jacksonJsonpMapper(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        return new JacksonJsonpMapper(jackson2ObjectMapperBuilder.build());
    }

    @Bean
    public RestClient restClient() {
        HttpHost httpHost = new HttpHost(elastic.getHost(), elastic.getPort(), elastic.getScheme());

        RestClientBuilder builder = RestClient.builder(httpHost)
            .setHttpClientConfigCallback(httpClientBuilder -> {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(elastic.getUserName(), elastic.getPassword())
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(5).build());
                return httpClientBuilder;
            });

        builder.setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout(elastic.getConnectTimeout())
                .setSocketTimeout(elastic.getSocketTimeout())
                .setConnectionRequestTimeout(elastic.getConnectRequestTimeout())
        );
        return builder.build();
    }
}
