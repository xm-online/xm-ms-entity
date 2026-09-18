package com.icthh.xm.ms.entity.config.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bohnman.squiggly.Squiggly;
import com.github.bohnman.squiggly.context.provider.SquigglyContextProvider;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import org.elasticsearch.client.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ElasticsearchConfiguration {

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate(Client client,
                                                       Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder,
                                                       SquigglyContextProvider contextProvider) {
        ObjectMapper objectMapper = jackson2ObjectMapperBuilder.createXmlMapper(false).build();
        return new ElasticsearchTemplate(client, new CustomEntityMapper(objectMapper, contextProvider));
    }

    /**
     * Small, dedicated, bounded executor used only to bound the runtime of individual
     * Elasticsearch calls (see {@link ElasticsearchQueryTimeoutGuard}). Kept isolated from the
     * Undertow worker pool and the general async executor so that ES-side stalls cannot exhaust
     * either of those.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService elasticsearchQueryExecutor(ApplicationProperties applicationProperties) {
        ApplicationProperties.Elasticsearch config = applicationProperties.getElasticsearch();
        AtomicInteger threadCount = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "es-query-" + threadCount.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
            config.getQueryExecutorPoolSize(),
            config.getQueryExecutorPoolSize(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(config.getQueryExecutorQueueCapacity()),
            threadFactory,
            new ThreadPoolExecutor.AbortPolicy()
        );
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

}
