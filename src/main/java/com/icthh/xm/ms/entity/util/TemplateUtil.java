package com.icthh.xm.ms.entity.util;

import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Slf4j
public final class TemplateUtil {

    private TemplateUtil() {
    }

    public static RestTemplate getTemplate(ClientHttpRequestInterceptor interceptor) {
        RestTemplate restTemplate = new RestTemplate();

        List<ClientHttpRequestInterceptor> ris = new ArrayList<>();
        ris.add(interceptor);
        restTemplate.setInterceptors(ris);
        SimpleClientHttpRequestFactory httpFactory = new SimpleClientHttpRequestFactory();
        httpFactory.setOutputStreaming(false);
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(httpFactory));
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        return restTemplate;
    }

    public static void disableSSL(RestTemplate restTemplate) {
        try {
            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
                .build();
            CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                .build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
            requestFactory.setHttpClient(httpClient);
            restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(requestFactory));
        } catch (Exception e) {
            log.error("Exception occurred while creating http factory, error={}", e.getMessage(), e);
        }
    }
}
