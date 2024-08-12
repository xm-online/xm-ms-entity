package com.icthh.xm.ms.entity.config;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
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
public class RestTemplateConfiguration {

    @Bean
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
        return restTemplate;
    }

    @Bean
    public RestTemplate loadBalancedRestTemplateWithTimeout(RestTemplateCustomizer customizer,
                                                            PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(requestFactory);
        customizer.customize(restTemplate);
        return restTemplate;
    }

    // To propagate traceId across third services with RestTemplate client,
    // rest template should be created using builder
    @Bean
    public RestTemplate plainRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public PathTimeoutHttpComponentsClientHttpRequestFactory pathTimeoutHttpComponentsClientHttpRequestFactory() {
        return new PathTimeoutHttpComponentsClientHttpRequestFactory();
    }

    public static class PathTimeoutHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {
        @Getter
        private final Set<PathTimeoutConfig> pathPatternTimeoutConfigs = new HashSet<>();
        private final AntPathMatcher matcher = new AntPathMatcher();

        @Override
        protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
            for (PathTimeoutConfig config : pathPatternTimeoutConfigs) {
                if (httpMethod.equals(config.getHttpMethod()) && matcher.match(config.getPathPattern(), uri.getPath())) {

                    RequestConfig.Builder builder = RequestConfig.custom();
                    setIfNotNull(config.getReadTimeout(),  builder::setResponseTimeout);
                    setIfNotNull(config.getConnectionTimeout(),  builder::setConnectTimeout);
                    setIfNotNull(config.getConnectionRequestTimeout(),  builder::setConnectionRequestTimeout);

                    HttpClientContext context = HttpClientContext.create();
                    context.setAttribute(HttpClientContext.REQUEST_CONFIG, builder.build());
                    return context;
                }
            }

            // Returning null allows HttpComponentsClientHttpRequestFactory to continue down normal path for populating the context
            return null;
        }

        private void setIfNotNull(Integer value, Consumer<Timeout> setterMethod) {
            if (value == null) {
                return;
            }
            setterMethod.accept(Timeout.ofMilliseconds(value));
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
