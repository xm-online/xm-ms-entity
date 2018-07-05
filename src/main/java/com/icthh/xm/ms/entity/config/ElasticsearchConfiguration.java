package com.icthh.xm.ms.entity.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.domain.Content;
import org.elasticsearch.client.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Configuration
public class ElasticsearchConfiguration {

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate(Client client,
                                                       Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        ObjectMapper objectMapper = jackson2ObjectMapperBuilder.createXmlMapper(false).build();
        return new ElasticsearchTemplate(client, new CustomEntityMapper(objectMapper));
    }

    @Component("indexName")
    public static class IndexName {

        private final TenantContextHolder tenantContextHolder;

        public IndexName(TenantContextHolder tenantContextHolder) {
            this.tenantContextHolder = tenantContextHolder;
        }

        private String getTenantKeyLowerCase() {
            return TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder).toLowerCase();
        }

        public String getPrefix() {
            return getTenantKeyLowerCase() + "_";
        }
    }

    public static class CustomEntityMapper implements EntityMapper {

        @JsonIgnoreProperties("value")
        abstract static class ContentValueFilter {}

        private ObjectMapper objectMapper;

        public CustomEntityMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            objectMapper.addMixIn(Content.class, ContentValueFilter.class);
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
