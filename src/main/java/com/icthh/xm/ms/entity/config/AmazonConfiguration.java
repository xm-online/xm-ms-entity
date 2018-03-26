package com.icthh.xm.ms.entity.config;

import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class AmazonConfiguration {

    private final ApplicationProperties applicationProperties;

    @Bean
    AmazonS3Template amazonS3Template() {
        return new AmazonS3Template(
            applicationProperties.getAmazon().getS3().getBucket(),
            applicationProperties.getAmazon().getAws().getEndpoint(),
            applicationProperties.getAmazon().getAws().getRegion(),
            applicationProperties.getAmazon().getAws().getAccessKeyId(),
            applicationProperties.getAmazon().getAws().getAccessKeySecret());
    }
}
