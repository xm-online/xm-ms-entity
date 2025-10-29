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
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
public class AmazonS3TemplateUnitTest extends AbstractJupiterUnitTest {

    private static final String PREFIX = "test";
    private static final String BUCKET = "bucket_for_TEST";
    private static final String KEY = "testkey";

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final AmazonS3Template s3Template = new AmazonS3Template(applicationProperties, this::createS3Client, new AmazonS3BucketNameFactory());

    @Container
    public static GenericContainer mockS3 = new GenericContainer("adobe/s3mock")
        .withCreateContainerCmdModifier(getContainerModifier()).withExposedPorts(9090);;

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

    @Test
    public void saveWithException() throws Exception {
        Assertions.assertThrows(IOException.class, () -> {
            String content = "content string";
            InputStream stream = IOUtils.toInputStream(content, StandardCharsets.UTF_8);
            s3Template.save("test-bucket-for-test-with-exception", "testkey", stream, content .getBytes().length, "filename");
        });
    }

    @Test
    public void saveAndLoadByLink() throws Exception {
        String content = "content string";
        UploadResultDto resultDto = uploadFileToS3(content);
        assertFileSaved(resultDto);

        Attachment attachment = new Attachment();
        attachment.setName("fileName.json");
        attachment.setContentChecksum(resultDto.getETag());
        attachment.setContentUrl(resultDto.getBucketName() + FileUtils.BUCKET_NAME_SEPARATOR + resultDto.getKey());

        URL expirableLink = s3Template.createExpirableLink(attachment, 100500L);
        log.info("link: {}", expirableLink);
        String value = IOUtils.toString(expirableLink, StandardCharsets.UTF_8);
        assertEquals(content, value);
    }

    @Test
    public void deleteByBucketAndKey() throws Exception {
        String content = "content string";
        UploadResultDto resultDto = uploadFileToS3(content);
        assertFileSaved(resultDto);

        s3Template.delete(resultDto.getBucketName(), resultDto.getKey());

        Attachment attachment = new Attachment();
        attachment.setName("fileName.json");
        attachment.setContentChecksum(resultDto.getETag());
        attachment.setContentUrl(resultDto.getBucketName() + FileUtils.BUCKET_NAME_SEPARATOR + resultDto.getKey());

        URL expirableLink = s3Template.createExpirableLink(attachment, 100500L);
        Exception exception = assertThrows(FileNotFoundException.class, () -> {
            IOUtils.toString(expirableLink, StandardCharsets.UTF_8);
        });

        assertEquals(FileNotFoundException.class, exception.getClass());
    }

    private UploadResultDto uploadFileToS3(String strContent) {
        log.info("{}", mockS3.isRunning());
        Content content = new Content();
        content.setValue(strContent.getBytes(StandardCharsets.UTF_8));
        s3Template.createBucketIfNotExist(PREFIX, BUCKET);
        return s3Template.save(prepareBucketName(), KEY, content, "filename");
    }

    private void assertFileSaved(UploadResultDto resultDto) {
        assertThat(resultDto).isNotNull();
        assertThat(resultDto.getKey()).isEqualTo(KEY);
        assertThat(resultDto.getBucketName()).isEqualTo(prepareBucketName());
        assertThat(resultDto.getETag()).isNotBlank();
    }

    private String prepareBucketName() {
        return PREFIX + "-" + BUCKET.toLowerCase().replace("_", "-");
    }
}
