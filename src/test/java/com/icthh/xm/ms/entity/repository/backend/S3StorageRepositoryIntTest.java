package com.icthh.xm.ms.entity.repository.backend;

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
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.lep.api.LepManager;
import com.icthh.xm.ms.entity.AbstractJupiterSpringBootTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3BucketNameFactory;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.FileUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_AUTH_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@Testcontainers
public class S3StorageRepositoryIntTest extends AbstractJupiterSpringBootTest {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private TenantContextHolder tenantContextHolder;
    @Autowired
    private LepManager lepManager;
    @Autowired
    private XmAuthenticationContextHolder authContextHolder;
    @Autowired
    private AmazonS3BucketNameFactory amazonS3BucketNameFactory;

    private S3StorageRepository s3StorageRepository;

    @Container
    public static GenericContainer<?> mockS3 = new GenericContainer<>("adobe/s3mock")
        .withCreateContainerCmdModifier(getContainerModifier())
        .withExposedPorts(9090);

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

    @BeforeEach
    public void setup() {

        TenantContextUtils.setTenant(tenantContextHolder, "TEST_TENANT");

        AmazonS3Template s3Template = new AmazonS3Template(applicationProperties, this::createS3Client, amazonS3BucketNameFactory);
        s3StorageRepository = new S3StorageRepository(applicationProperties, s3Template, tenantContextHolder);

        lepManager.beginThreadContext(ctx -> {
            ctx.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            ctx.setValue(THREAD_CONTEXT_KEY_AUTH_CONTEXT, authContextHolder.getContext());
        });
    }

    @AfterEach
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test

    @SneakyThrows
    public void store() {
        String folderName = "XM_ENTITY_TYPE_KEY";
        String fileName = "Декларація 2020.pdf";

        InputStream inputStream = new ClassPathResource("testfiles/" + fileName).getInputStream();
        Content content = new Content();
        content.setValue(IOUtils.toByteArray(inputStream));

        UploadResultDto store = s3StorageRepository.store(content, folderName, fileName);
        assertNotNull(store);

        Attachment attachment = new Attachment();
        attachment.setName(fileName);
        attachment.setContent(content);
        attachment.setValueContentSize((long) content.getValue().length);
        attachment.setContentChecksum(store.getETag());
        attachment.setValueContentType(URLConnection.guessContentTypeFromStream(inputStream));
        attachment.setContentUrl(store.getBucketName() + FileUtils.FILE_NAME_SEPARATOR + store.getKey());

        URL expirableLink = s3StorageRepository.createExpirableLink(attachment, 3000000L);

        BasicNameValuePair basicNameValuePair = new BasicNameValuePair("response-content-disposition", FileUtils.getContentDisposition(attachment.getName()));
        List<NameValuePair> parse = URLEncodedUtils.parse(expirableLink.getQuery(), StandardCharsets.UTF_8);

        assertThat(parse).contains(basicNameValuePair);
    }
}
