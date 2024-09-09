package com.icthh.xm.ms.entity.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class LoadBalancerConfiguration {

    private final String SERVICE_ID = "entity";

    @Bean
    public LoadBalancerClientFactory loadBalancerClientFactory() throws URISyntaxException {
        LoadBalancerClientFactory factory = mock(LoadBalancerClientFactory.class);

        ServiceInstance serviceInstance = mockServiceInstance();
        Mono<Response<ServiceInstance>> responseMono = Mono.just(new DefaultResponse(serviceInstance));

        ReactiveLoadBalancer reactiveLoadBalancer = mock(ReactiveLoadBalancer.class);
        given(reactiveLoadBalancer.choose(any(Request.class))).willReturn(responseMono);
        given(factory.getInstance(eq(SERVICE_ID))).willReturn(reactiveLoadBalancer);

        LoadBalancerProperties properties = mockLoadBalancerProperties();
        given(factory.getProperties(eq(SERVICE_ID))).willReturn(properties);

        Map<String, Object> map = new HashMap<>();
        given(factory.getInstances(any(), any())).willReturn(map);

        return factory;
    }

    @Bean
    public LoadBalancerRequestTransformer loadBalancerRequestTransformer() {
        return new LoadBalancerRequestTransformer() {
            @Override
            public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
                return new HttpRequestWrapper(request) {
                    @Override
                    public URI getURI() {
                        return UriComponentsBuilder.fromUri(request.getURI())
                            .host("localhost")
                            .port(8081)
                            .build()
                            .toUri();
                    }
                };
            }
        };
    }

    private LoadBalancerProperties mockLoadBalancerProperties() {
        LoadBalancerProperties properties = mock(LoadBalancerProperties.class);
        given(properties.getRetry()).willReturn(new LoadBalancerProperties.Retry());
        given(properties.getStickySession()).willReturn(new LoadBalancerProperties.StickySession());
        given(properties.getXForwarded()).willReturn(new LoadBalancerProperties.XForwarded());
        return properties;
    }

    private ServiceInstance mockServiceInstance() throws URISyntaxException {
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        given(serviceInstance.getServiceId()).willReturn(SERVICE_ID);
        given(serviceInstance.getUri()).willReturn(new URI("http://localhost:8081"));
        return serviceInstance;
    }
}
