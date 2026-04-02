package com.icthh.xm.ms.entity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.LoadBalancerConfiguration;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory;
import com.icthh.xm.ms.entity.config.RestTemplateConfiguration.PathTimeoutHttpComponentsClientHttpRequestFactory.PathTimeoutConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@ContextConfiguration(classes = {LoadBalancerConfiguration.class})
@Disabled
//TODO Migrate to test container
public class RestTimeoutCustomizerIntTest extends AbstractJupiterSpringBootTest {

    @Qualifier("loadBalancedRestTemplateWithTimeout")
    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PathTimeoutHttpComponentsClientHttpRequestFactory requestFactory;

    private MockWebServer mockWebServer;

    @BeforeEach
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(8081);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    public void testCallWithTimeout() {
        Assertions.assertThrows(ResourceAccessException.class, () -> {
            String expectedUri = "http://localhost:8081/test/sleep";

            mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBodyDelay(100, java.util.concurrent.TimeUnit.MILLISECONDS));
            requestFactory.addPathTimeoutConfig(PathTimeoutConfig.builder()
                .httpMethod(HttpMethod.GET)
                .pathPattern("/test/sleep")
                .readTimeout(50).build());
            restTemplate.getForObject(expectedUri, Void.class);
        });

    }

    @Test
    public void testCallWithoutTimeout() throws Exception {
        String expectedUri = "http://localhost:8081/test/sleep";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        requestFactory.addPathTimeoutConfig(PathTimeoutConfig.builder()
                                                             .httpMethod(HttpMethod.GET)
                                                             .pathPattern("/test/sleep")
                                                             .readTimeout(50).build());
        restTemplate.getForObject(expectedUri, Void.class);
        
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertNotNull(recordedRequest);
        assertEquals("/test/sleep", recordedRequest.getPath());
    }

}
