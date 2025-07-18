package com.icthh.xm.ms.entity.config;

import static org.apache.http.client.protocol.HttpClientContext.REQUEST_CONFIG;

import com.icthh.xm.ms.entity.config.ApplicationProperties.GlobalEntityTimeoutProperties;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate beans.
 */
@Configuration
@Slf4j
public class RestTemplateConfiguration {

    @Value("${ribbon.http.client.enabled:true}")
    private Boolean ribbonTemplateEnabled;

    @Bean
    public RestTemplate loadBalancedRestTemplate(ApplicationProperties applicationProperties,
        RestTemplateBuilder restTemplateBuilder,
        ObjectProvider<RestTemplateCustomizer> customizerProvider) {
        RestTemplate restTemplate = createRestTemplate(restTemplateBuilder,
            applicationProperties.getGlobalEntityTimeouts());

        if (ribbonTemplateEnabled) {
            log.info("loadBalancedRestTemplate: using Ribbon load balancer");
            customizerProvider.ifAvailable(customizer -> customizer.customize(restTemplate));
        }

        return restTemplate;
    }

    @Bean
    public RestTemplate loadBalancedRestTemplateWithTimeoutByPath(ApplicationProperties applicationProperties,
        RestTemplateBuilder restTemplateBuilder,
        ObjectProvider<RestTemplateCustomizer> customizerProvider,
        PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory) {
        RestTemplate restTemplate = createRestTemplate(restTemplateBuilder,
            applicationProperties.getGlobalEntityTimeouts());
        restTemplate.setRequestFactory(requestFactory);

        if (ribbonTemplateEnabled) {
            log.info("loadBalancedRestTemplateWithTimeout: using Ribbon load balancer");
            customizerProvider.ifAvailable(customizer -> customizer.customize(restTemplate));
        }

        return restTemplate;
    }

    @Bean
    public RestTemplate plainRestTemplate(RestTemplateBuilder restTemplateBuilder,
        ApplicationProperties applicationProperties) {
        log.info("plainRestTemplate creation without loadBalancer started");
        return createRestTemplate(restTemplateBuilder, applicationProperties.getGlobalEntityTimeouts());
    }

    @Bean
    public PathTimeoutHttpComponentsClientHttpRequestFactory pathTimeoutHttpComponentsClientHttpRequestFactory() {
        return new PathTimeoutHttpComponentsClientHttpRequestFactory();
    }

    private RestTemplate createRestTemplate(RestTemplateBuilder restTemplateBuilder,
        GlobalEntityTimeoutProperties entityTimeoutProperties) {

        if (
            Objects.nonNull(entityTimeoutProperties) &&
                ObjectUtils.anyNotNull(
                    entityTimeoutProperties.getConnectionTimeout(),
                    entityTimeoutProperties.getReadTimeout()
                )
        ) {
            log.info("entityCreateRestTemplate with timeouts={}", entityTimeoutProperties);
            return restTemplateBuilder
                .setConnectTimeout(entityTimeoutProperties.getConnectionTimeout())
                .setReadTimeout(entityTimeoutProperties.getReadTimeout())
                .build();
        }
        log.info("entityCreateRestTemplate without timeouts");
        return restTemplateBuilder.build();
    }

    public static class PathTimeoutHttpComponentsClientHttpRequestFactory extends
        HttpComponentsClientHttpRequestFactory {

        @Getter
        private final Set<PathTimeoutConfig> pathPatternTimeoutConfigs = new HashSet<>();
        private final AntPathMatcher matcher = new AntPathMatcher();

        @Override
        protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
            for (PathTimeoutConfig config : pathPatternTimeoutConfigs) {
                if (httpMethod.equals(config.getHttpMethod()) && matcher.match(config.getPathPattern(),
                    uri.getPath())) {
                    RequestConfig requestConfig = createRequestConfig(getHttpClient());
                    RequestConfig.Builder builder = RequestConfig.copy(requestConfig);
                    setIfNotNull(config.getReadTimeout(), builder::setSocketTimeout);
                    setIfNotNull(config.getConnectionTimeout(), builder::setConnectTimeout);
                    setIfNotNull(config.getConnectionRequestTimeout(), builder::setConnectionRequestTimeout);

                    HttpClientContext context = HttpClientContext.create();
                    context.setAttribute(REQUEST_CONFIG, builder.build());
                    return context;
                }
            }

            // Returning null allows HttpComponentsClientHttpRequestFactory to continue down normal path for populating the context
            return null;
        }

        private <T> void setIfNotNull(T value, Consumer<T> setterMethod) {
            if (value == null) {
                return;
            }
            setterMethod.accept(value);
        }

        public void addPathTimeoutConfig(PathTimeoutConfig pathTimeoutConfig) {
            pathPatternTimeoutConfigs.add(pathTimeoutConfig);
        }

        @Data
        @Builder
        @AllArgsConstructor
        @EqualsAndHashCode(of = {"httpMethod", "pathPattern"})
        public static class PathTimeoutConfig {

            private HttpMethod httpMethod;
            private String pathPattern;
            private Integer connectionTimeout;
            private Integer connectionRequestTimeout;
            private Integer readTimeout;
        }
    }
}
