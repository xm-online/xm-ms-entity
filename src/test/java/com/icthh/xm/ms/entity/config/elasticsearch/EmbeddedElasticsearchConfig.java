package com.icthh.xm.ms.entity.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.NodeClientFactoryBean;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@TestConfiguration
@EnableConfigurationProperties(ElasticsearchProperties.class)
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

    @Bean
    public ElasticsearchClient elasticsearchClientTest() {
        return new ElasticsearchClient(elasticsearchTransport());
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport() {
        return new RestClientTransport(restClient(), jacksonJsonpMapper());
    }

    @Bean
    public JacksonJsonpMapper jacksonJsonpMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        objectMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new JacksonJsonpMapper(objectMapper);
    }

    @Bean
    public RestClient restClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost("localhost", 9200))
            .setHttpClientConfigCallback(httpClientBuilder -> {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials("admin", "admin")
                );
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                httpClientBuilder.setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(5).build());
                return httpClientBuilder;
            });

        builder.setRequestConfigCallback(requestConfigBuilder ->
            requestConfigBuilder
                .setConnectTimeout(5000)
                .setSocketTimeout(30000)
                .setConnectionRequestTimeout(30000)
        );
        return builder.build();
    }

}
