package com.icthh.xm.ms.entity.repository.backend;

import com.google.common.hash.Hashing;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.FileUtils;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileStorageRepository implements StorageRepository {

    private final ApplicationProperties applicationProperties;
    private final FilePrefixNameFactory  filePrefixNameFactory;
    private final TenantContextHolder tenantContextHolder;

    /**
     * we should allow only letters, numbers and '/' symbol
     */
    private static final String FOLDER_NAME = "^[a-zA-Z0-9/]+$";
    private static final Pattern pattern = Pattern.compile(FOLDER_NAME);

    @Override
    public String store(MultipartFile file, Integer size) {
        return "";
    }

    @Override
    @SneakyThrows
    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        // Get the resource from HttpEntity
        Resource resource = httpEntity.getBody();

        if (resource == null) {
            log.error("Resource in HttpEntity is null");
            throw new BusinessException("storage.file.validation", "Resource in HttpEntity is null");
        }

        // Get the file template path from configurationC
        String randomFilePrefix = Hashing.murmur3_128().hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8).toString();
        //File will be named {mumurHash}-originalFileName
        String fileName = randomFilePrefix + "-" + XmHttpEntityUtils.getFileName(httpEntity.getHeaders());

        String tenantName = tenantContextHolder.getTenantKey().toLowerCase();
        final String fileTemplate = applicationProperties.getObjectStorage().getFileRoot() + "/" + tenantName;

        String subfolder = filePrefixNameFactory.evaluatePathSubfolder();

        if (StringUtils.isNotEmpty(subfolder)) {
            Matcher matcher = pattern.matcher(subfolder);
            boolean isValid = matcher.matches();
            if (!isValid) {
                throw new BusinessException("storage.file.subfolder.validation", "Subfolder " + subfolder + " is not matched for pattern \"" + FOLDER_NAME + "\"");
            }
        }

        Path filePath = Paths.get(fileTemplate + subfolder + "/" + fileName);

        // Create parent directories if they don't exist
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Created directory: {}", parentDir);
        }

        // Save the resource to file
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Successfully stored file to: {}", filePath);
        }

        //??? "fs://" + subfolder + "/" + fileName
        return "file://" + fileName;
    }

    @SneakyThrows
    public Resource getFileFromFs(String fileName) {
        String tenantName = tenantContextHolder.getTenantKey().toLowerCase();
        final String fileTemplate = applicationProperties.getObjectStorage().getFileRoot() + "/" + tenantName;
        String subfolder = filePrefixNameFactory.evaluatePathSubfolder();

        if (StringUtils.isNotEmpty(subfolder)) {
            Matcher matcher = pattern.matcher(subfolder);
            boolean isValid = matcher.matches();
            if (!isValid) {
                throw new BusinessException("storage.file.subfolder.validation", "Subfolder " + subfolder + " is not matched for pattern \"" + FOLDER_NAME + "\"");
            }
        }

        Path filePath = Paths.get(fileTemplate + subfolder + "/" + fileName);
        return new ByteArrayResource(Files.readAllBytes(filePath));
    }

    @Override
    public UploadResultDto store(Content content, String folderName, String fileName) {
        return null;
    }
}
