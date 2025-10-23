package com.icthh.xm.ms.entity.repository.backend;

import com.google.common.hash.Hashing;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;

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

        Path filePath = getFilePath(fileName, null);

        log.info("store path: {}", filePath);
        // Create parent directories if they don't exist
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Created directory: {}", parentDir);
        }

        // Save the resource to file
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("stored file to: {}", filePath);
        }

        return FILE_PREFIX + fileName;
    }

    @SneakyThrows
    public Resource getFileFromFs(String fileContentUrl) {
        //fileName contains file name and details, the tenant subfolder is evaluated by getFilePath() function
        String simpleFileName = fileContentUrl;
        if (StringUtils.startsWith(fileContentUrl, FILE_PREFIX)) {
            simpleFileName = StringUtils.substringAfter(fileContentUrl, FILE_PREFIX);
        }

        Path filePath = getFilePath(simpleFileName, null);
        log.info("reading file {}", filePath);
        return new ByteArrayResource(Files.readAllBytes(filePath));
    }

    @Override
    @SneakyThrows
    public UploadResultDto store(Content content, String folderName, String fileName) {
        // Get the file template path from configurationC
        String randomFilePrefix = Hashing.murmur3_128().hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8).toString();
        //File will be named {mumurHash}-originalFileName
        String fullFileName = randomFilePrefix + "-" + fileName;

        if (StringUtils.isNotEmpty(folderName)) {
            fullFileName = folderName + "/" + fullFileName;
        }

        String subfolder = filePrefixNameFactory.evaluatePathSubfolder();

        Path filePath = getFilePath(fullFileName, subfolder);

        // Create parent directories if they don't exist
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Created directory: {}", parentDir);
        }

        Files.write(filePath, content.getValue(), StandardOpenOption.CREATE);

        String fileContentName = FILE_PREFIX + fullFileName;
        return new UploadResultDto(subfolder, fileContentName, DigestUtils.sha256Hex(content.getValue()));
    }

    @Override
    public void delete(String contentUrl) {
        //fileName contains file name and details, tenant subfolder is evaluated by getFilePath() function
        String simpleFileName = StringUtils.substringAfter(contentUrl, FILE_PREFIX);
        Path filePath = getFilePath(simpleFileName, null);
        org.apache.commons.io.FileUtils.deleteQuietly(filePath.toFile());
    }

    private Path getFilePath(String fileName, String subfolder) {
        String tenantName = tenantContextHolder.getTenantKey().toLowerCase();
        final String fileTemplate = applicationProperties.getObjectStorage().getFileRoot() + "/" + tenantName;

        if (StringUtils.isEmpty(subfolder)) {
            subfolder = filePrefixNameFactory.evaluatePathSubfolder();
        }

        if (StringUtils.isNotEmpty(subfolder)) {
            Matcher matcher = pattern.matcher(subfolder);
            boolean isValid = matcher.matches();
            if (!isValid) {
                throw new BusinessException("storage.file.subfolder.validation", "Subfolder " + subfolder + " is not matched for pattern \"" + FOLDER_NAME + "\"");
            }
        }
        return Paths.get(fileTemplate + subfolder + "/" + fileName);
    }
}
