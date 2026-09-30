package com.icthh.xm.ms.entity.repository.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FsFileStorageRepositoryUnitTest extends AbstractJupiterUnitTest {

    @TempDir
    Path root;

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private FilePrefixNameFactory filePrefixNameFactory;
    @Mock
    private TenantContextHolder tenantContextHolder;

    private FsFileStorageRepository repository;

    @BeforeEach
    public void before() throws Exception {
        MockitoAnnotations.openMocks(this);
        ApplicationProperties.ObjectStorage objectStorage = new ApplicationProperties.ObjectStorage();
        objectStorage.setFileRoot(root.toString());
        when(applicationProperties.getObjectStorage()).thenReturn(objectStorage);
        when(tenantContextHolder.getTenantKey()).thenReturn("TEST");
        repository = new FsFileStorageRepository(applicationProperties, filePrefixNameFactory, tenantContextHolder);

        Files.createDirectories(root.resolve("test/sub"));
        Files.writeString(root.resolve("test/sub/file.txt"), "tenant", StandardCharsets.UTF_8);
        Files.createDirectories(root.resolve("other"));
        Files.writeString(root.resolve("other/secret.txt"), "secret", StandardCharsets.UTF_8);
    }

    @Test
    public void shouldReadFileInsideTenantFolder() throws Exception {
        assertThat(repository.getFileFromFs("file://sub/file.txt").getContentAsByteArray())
            .isEqualTo("tenant".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldRejectPathTraversalOnRead() {
        assertThrows(BusinessException.class, () -> repository.getFileFromFs("file://../other/secret.txt"));
        assertThrows(BusinessException.class, () -> repository.getFileFromFs("sub/../../other/secret.txt"));
    }

    @Test
    public void shouldRejectPathTraversalOnDelete() {
        assertThrows(BusinessException.class, () -> repository.delete("file://../other/secret.txt"));
        assertThat(Files.exists(root.resolve("other/secret.txt"))).isTrue();
    }
}
