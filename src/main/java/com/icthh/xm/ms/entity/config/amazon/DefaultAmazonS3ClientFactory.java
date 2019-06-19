package com.icthh.xm.ms.entity.config.amazon;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.ApplicationProperties.Amazon.Aws;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Lazy
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAmazonS3ClientFactory implements AmazonS3ClientFactory {

    private final ApplicationProperties applicationProperties;

    @Getter
    private final AmazonS3 amazonS3;

    public DefaultAmazonS3ClientFactory(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.amazonS3 = createAmazonS3Client();
    }

    protected AmazonS3 createAmazonS3Client() {
        Aws aws = applicationProperties.getAmazon().getAws();
        log.info("Init amazon s3 client with args {}", aws);
        BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(aws.getAccessKeyId(), aws.getAccessKeySecret());
        return AmazonS3ClientBuilder.standard()
                                    .withEndpointConfiguration(new EndpointConfiguration(aws.getEndpoint(), aws.getRegion()))
                                    .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                                    .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                                    .build();
    }

}
