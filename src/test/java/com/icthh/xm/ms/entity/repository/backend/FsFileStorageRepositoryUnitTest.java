package com.icthh.xm.ms.entity.repository.backend;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;
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
    public void shouldReadFileOutsideTenantFolderWhenTenantAccessCheckIsOff() throws Exception {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(false);
        assertThat(repository.getFileFromFs("file://../other/secret.txt").getContentAsByteArray())
            .isEqualTo("secret".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldReadFileInsideTenantFolderWhenTenantAccessCheckIsOn() throws Exception {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        assertThat(repository.getFileFromFs("file://sub/file.txt").getContentAsByteArray())
            .isEqualTo("tenant".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void shouldRejectPathOutsideTenantFolderWhenTenantAccessCheckIsOn() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        assertThrows(BusinessException.class, () -> repository.getFileFromFs("file://../other/secret.txt"));
        assertThrows(BusinessException.class, () -> repository.getFileFromFs("sub/../../other/secret.txt"));
        assertThrows(BusinessException.class, () -> repository.delete("file://../other/secret.txt"));
        assertThat(Files.exists(root.resolve("other/secret.txt"))).isTrue();
    }

    @Test
    public void shouldRejectDirectoryOnDelete() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        when(applicationProperties.isSecureAttachmentContentUrl()).thenReturn(true);
        assertThrows(BusinessException.class, () -> repository.delete("file://sub"));
        assertThat(Files.exists(root.resolve("test/sub/file.txt"))).isTrue();
    }

    @Test
    public void shouldDeleteDirectoryWhenContentUrlCheckIsOff() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        when(applicationProperties.isSecureAttachmentContentUrl()).thenReturn(false);
        repository.delete("file://sub");
        assertThat(Files.exists(root.resolve("test/sub"))).isFalse();
    }

    @Test
    public void shouldDeleteFileAndIgnoreMissingFile() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        repository.delete("file://sub/file.txt");
        assertThat(Files.exists(root.resolve("test/sub/file.txt"))).isFalse();
        repository.delete("file://sub/file.txt");
    }

    @Test
    public void shouldStoreContentByFileNameOnly() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        Content content = new Content();
        content.setValue("attacker".getBytes(StandardCharsets.UTF_8));

        String key = repository.store(content, "T", "x/../../sub/file.txt").getKey();

        assertThat(key).startsWith(FILE_PREFIX + "T/").endsWith("-file.txt").doesNotContain("..");
        assertThat(root.resolve("test").resolve(StringUtils.substringAfter(key, FILE_PREFIX))).hasContent("attacker");
        assertThat(root.resolve("test/sub/file.txt")).hasContent("tenant");
    }

    @Test
    public void shouldCheckContentUrlOnSaveOnlyWhenTenantAccessCheckIsOn() {
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(false);
        repository.checkTenantContentUrl("file://../other/secret.txt");
        when(applicationProperties.isSecureAttachmentTenantAccess()).thenReturn(true);
        repository.checkTenantContentUrl("file://sub/file.txt");
        assertThrows(BusinessException.class, () -> repository.checkTenantContentUrl("file://../other/secret.txt"));
    }
}
