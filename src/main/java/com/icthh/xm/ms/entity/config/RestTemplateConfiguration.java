package com.icthh.xm.ms.entity.config;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for RestTemplate beans.
 */
@Configuration
public class RestTemplateConfiguration {

    private static final int DEFAULT_TIMEOUT_MS = 30000;

    @LoadBalanced
    @Bean
    public RestTemplate loadBalancedRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @LoadBalanced
    @Bean
    public RestTemplate loadBalancedRestTemplateWithTimeout(RestTemplateBuilder restTemplateBuilder,
                                                            PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory) {
        return restTemplateBuilder
            .requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
            .build();
    }

    /**
     * To propagate traceId across third services with RestTemplate client,
     * rest template should be created using builder
     */
    @Bean
    public RestTemplate plainRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    @Bean
    public PathTimeoutHttpComponentsClientHttpRequestFactory pathTimeoutHttpComponentsClientHttpRequestFactory() {
        return new PathTimeoutHttpComponentsClientHttpRequestFactory();
    }

    public static class PathTimeoutHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {
        @Getter
        private final Set<PathTimeoutConfig> pathPatternTimeoutConfigs = new HashSet<>();
        private final AntPathMatcher matcher = new AntPathMatcher();

        public PathTimeoutHttpComponentsClientHttpRequestFactory() {
            super();
            
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(DEFAULT_TIMEOUT_MS))
                .setSocketTimeout(Timeout.ofMilliseconds(DEFAULT_TIMEOUT_MS))
                .build());
            
            CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(DEFAULT_TIMEOUT_MS))
                    .build())
                .build();
            
            this.setHttpClient(httpClient);
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
