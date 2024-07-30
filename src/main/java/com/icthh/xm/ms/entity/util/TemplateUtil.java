//package com.icthh.xm.ms.entity.util;
//
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.security.KeyManagementException;
//import java.security.NoSuchAlgorithmException;
//import java.security.cert.CertificateException;
//import java.security.cert.X509Certificate;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManager;
//import javax.net.ssl.X509ExtendedTrustManager;
//import javax.net.ssl.X509TrustManager;
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.http.client.HttpClient;
//import org.apache.http.conn.ssl.NoopHostnameVerifier;
//import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.http.client.BufferingClientHttpRequestFactory;
//import org.springframework.http.client.ClientHttpRequestInterceptor;
//import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
//import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.http.converter.StringHttpMessageConverter;
//import org.springframework.web.client.RestTemplate;
//
//@Slf4j
//public final class TemplateUtil {
//
//    private TemplateUtil() {
//    }
//
//    public static RestTemplate getTemplate(ClientHttpRequestInterceptor interceptor) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        List<ClientHttpRequestInterceptor> ris = new ArrayList<>();
//        ris.add(interceptor);
//        restTemplate.setInterceptors(ris);
//        SimpleClientHttpRequestFactory httpFactory = new SimpleClientHttpRequestFactory();
////        httpFactory.setOutputStreaming(false);
//        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(httpFactory));
//        restTemplate.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
//        return restTemplate;
//    }
//
//    public static void disableSSL(RestTemplate restTemplate) {
//        try {
//            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
//                .loadTrustMaterial(null, (X509Certificate[] chain, String authType) -> true)
//                .build();
//            HttpClient httpClient = HttpClients.custom()
//                .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
//                .build();
//            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
////            requestFactory.setHttpClient(httpClient);
//            restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(requestFactory));
//        } catch (Exception e) {
//            log.error("Exception occurred while creating http factory, error={}", e.getMessage(), e);
//        }
//    }
//
//    public void getX509TrustManager() {
//        X509TrustManager finalDefaultTm = defaultX509CertificateTrustManager;
//        X509TrustManager finalMyTm = myTrustManager;
//
//        X509TrustManager wrapper = new X509TrustManager() {
//            private X509Certificate[] mergeCertificates() {
//                ArrayList<X509Certificate> resultingCerts = new ArrayList<>();
//                resultingCerts.addAll(Arrays.asList(finalDefaultTm.getAcceptedIssuers()));
//                resultingCerts.addAll(Arrays.asList(finalMyTm.getAcceptedIssuers()));
//                return resultingCerts.toArray(new X509Certificate[resultingCerts.size()]);
//            }
//
//            @Override
//            public X509Certificate[] getAcceptedIssuers() {
//                return mergeCertificates();
//            }
//
//            @Override
//            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//                try {
//                    finalMyTm.checkServerTrusted(chain, authType);
//                } catch (CertificateException e) {
//                    finalDefaultTm.checkServerTrusted(chain, authType);
//                }
//            }
//
//            @Override
//            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
//                finalDefaultTm.checkClientTrusted(mergeCertificates(), authType);
//            }
//        };
//    }
//
//    public RestTemplate getRestTemplate() throws NoSuchAlgorithmException, KeyManagementException {
//        // Initialize SSL context with the defined trust managers
//        SSLContext sslContext = SSLContext.getInstance("TLS");
//
//        // Define trust managers to accept all certificates
//        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
//            // Method to check client's trust - accepting all certificates
//            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
//            }
//
//            // Method to check server's trust - accepting all certificates
//            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
//            }
//
//            // Method to get accepted issuers - returning an empty array
//            public X509Certificate[] getAcceptedIssuers() {
//                return new X509Certificate[0];
//            }
//        }};
//
//        sslContext.init(null, trustManagers, null);
//
//        // Disable SSL verification for RestTemplate
//        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
//
//        // Create a RestTemplate with a custom request factory
//       return new RestTemplateBuilder()
//           .requestFactory(SimpleClientHttpRequestFactory.class)
//           .build();
//    }
//}
