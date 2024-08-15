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
import com.icthh.xm.ms.entity.config.LoadBalancerConfiguration;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory.PathTimeoutConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@ContextConfiguration(classes = {LoadBalancerConfiguration.class})
public class RestTimeoutCustomizerIntTest extends AbstractSpringBootTest {

    @Qualifier("loadBalancedRestTemplateWithTimeout")
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8081);

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

}
