package com.icthh.xm.ms.entity.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.bohnman.squiggly.Squiggly;
import com.github.bohnman.squiggly.context.provider.SquigglyContextProvider;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.data.elasticsearch.core.ResultsMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Configuration
public class ElasticsearchConfiguration {

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate(Client client,
                                                       Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder,
                                                       SquigglyContextProvider contextProvider) {
        ObjectMapper objectMapper = jackson2ObjectMapperBuilder.createXmlMapper(false).build();
        return new ElasticsearchTemplate(client, new CustomEntityMapper(objectMapper, contextProvider));
    }

    @Bean
    public ResultsMapper resultsMapper(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder,
                                       SquigglyContextProvider contextProvider) {
        ObjectMapper objectMapper = jackson2ObjectMapperBuilder.createXmlMapper(false).build();
        return new DefaultResultMapper(new CustomEntityMapper(objectMapper, contextProvider));
    }

    @Component("indexName")
    public static class IndexName {

        private final TenantContextHolder tenantContextHolder;
        private final ApplicationProperties applicationProperties;

        public IndexName(TenantContextHolder tenantContextHolder, ApplicationProperties applicationProperties) {
            this.tenantContextHolder = tenantContextHolder;
            this.applicationProperties = applicationProperties;
        }

        private String getTenantKeyLowerCase() {
            return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toLowerCase();
        }

        public String getPrefix() {
            return getTenantKeyLowerCase() + "_" + getElasticSchemaSuffix();
        }

        public String getElasticSchemaSuffix() {
            String suffix = applicationProperties.getElasticSchemaSuffix();
            return suffix == null ? "" : suffix;
        }
    }

    public static class CustomEntityMapper implements EntityMapper {

        @JsonIgnoreProperties("value")
        abstract static class ContentValueFilter {}

        private ObjectMapper objectMapper;

        public CustomEntityMapper(ObjectMapper objectMapper,
                                  final SquigglyContextProvider contextProvider) {
            this.objectMapper = objectMapper;
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            objectMapper.addMixIn(Content.class, ContentValueFilter.class);
            Squiggly.init(objectMapper, contextProvider);
        }

        @Override
        public String mapToString(Object object) throws IOException {
            return objectMapper.writeValueAsString(object);
        }

        @Override
        public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
            return objectMapper.readValue(source, clazz);
        }

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
