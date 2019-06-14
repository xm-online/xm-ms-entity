package com.icthh.xm.ms.entity.config.amazon;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Region;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

@Slf4j
public class AmazonS3TemplateIntTest{

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final AmazonS3Template s3Template = new AmazonS3Template(applicationProperties, this::createS3Client);

    @ClassRule
    public static GenericContainer mockS3 = new GenericContainer("adobe/s3mock")
        .withCreateContainerCmdModifier(getContainerModifier()).withExposedPorts(9090);

    private static Consumer<CreateContainerCmd> getContainerModifier() {
        return containerCmd -> containerCmd
            .withPortBindings(new PortBinding(Ports.Binding.bindPort(9191), new ExposedPort(9191)))
            .withPortBindings(new PortBinding(Ports.Binding.bindPort(9090), new ExposedPort(9090)));
    }

    public AmazonS3 createS3Client() {
        final BasicAWSCredentials credentials = new BasicAWSCredentials("foo", "bar");

        return AmazonS3ClientBuilder.standard()
                                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                                    .enablePathStyleAccess()
                                    .withEndpointConfiguration(
                                        new AwsClientBuilder.EndpointConfiguration(
                                            "http://127.0.0.1:9090",
                                            Region.US_Standard.getFirstRegionId()
                                        )).build();
    }

    @Test(expected = IOException.class)
    public void saveWithException() throws Exception {
        String content = "content string";
        InputStream stream = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        s3Template.save("test-bucket-for-test-with-exception", "testkey", stream, content .getBytes().length, "filename");
    }

    @Test
    public void saveAndLoadByLink() throws Exception {
        log.info("{}", mockS3.isRunning());
        String content = "content string";
        InputStream stream = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        s3Template.createBucketIfNotExist("test-", "bucket_for_TEST");
        s3Template.save("test-bucket-for-test", "testkey", stream, content .getBytes().length, "filename");
        URL expirableLink = s3Template.createExpirableLink("test-bucket-for-test", "testkey", 100500L);
        log.info("link: {}", expirableLink);
        String value = IOUtils.toString(expirableLink, StandardCharsets.UTF_8);
        Assert.assertEquals(content, value);
    }


}
