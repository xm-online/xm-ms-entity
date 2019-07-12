package com.icthh.xm.ms.entity.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory.PathTimeoutConfig;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class RestTimeoutCustomizerIntTest extends AbstractSpringBootTest {

    @Qualifier("loadBalancedRestTemplateWithTimeout")
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory;

    @ClassRule
    public static WireMockRule WIRE_MOCK = new WireMockRule();

    @Before
    public void setUp() {
    }

    @Test(expected = ResourceAccessException.class)
    public void testCallWithTimeout() throws Exception {
        String expectedUri = "http://entity/test/sleep";

        stubFor(get(urlEqualTo("/test/sleep")).willReturn(aResponse().withFixedDelay(100).withStatus(200)));
        requestFactory.addPathTimeoutConfig(PathTimeoutConfig.builder()
                                                             .httpMethod(HttpMethod.GET)
                                                             .pathPattern("/test/sleep")
                                                             .readTimeout(50).build());
        restTemplate.getForObject(expectedUri, Void.class);
    }

    @Test
    public void testCallWithoutTimeout() throws Exception {
        String expectedUri = "http://entity/test/sleep";
        stubFor(get(urlEqualTo("/test/sleep")).willReturn(aResponse().withStatus(200)));
        requestFactory.addPathTimeoutConfig(PathTimeoutConfig.builder()
                                                             .httpMethod(HttpMethod.GET)
                                                             .pathPattern("/test/sleep")
                                                             .readTimeout(50).build());
        restTemplate.getForObject(expectedUri, Void.class);
        verify(getRequestedFor(urlMatching("/test/sleep")));
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public ServerList<Server> ribbonServerList() {
            return new StaticServerList<>(new Server("localhost", WIRE_MOCK.port()));
        }
    }

}
