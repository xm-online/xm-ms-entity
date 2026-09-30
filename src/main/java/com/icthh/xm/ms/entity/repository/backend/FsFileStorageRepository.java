package com.icthh.xm.ms.entity.repository.backend;

import com.google.common.hash.Hashing;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;

@Slf4j
@RequiredArgsConstructor
@Component
public class FsFileStorageRepository implements StorageRepository {

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
        throw new UnsupportedOperationException("MultipartFile is not supported");
    }

    @Override
    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        // Get the resource from HttpEntity
        Resource resource = httpEntity.getBody();

        Objects.requireNonNull(resource, "Resource in HttpEntity is null");

        String fileName = FilenameUtils.getName(XmHttpEntityUtils.getFileName(httpEntity.getHeaders()));
        String fullFileName = fileNameWithSubfolder(fileName);

        try {
            return storeFileContent(fullFileName, resource.getContentAsByteArray());
        } catch (Exception e) {
            log.error("Error storing file", e);
            throw new BusinessException("error.store.file", "File storage error");
        }
    }

    @Override
    public UploadResultDto store(Content content, String folderName, String fileName) {

        String fullFileName = fileNameWithSubfolder(fileName);

        if (StringUtils.isNotEmpty(folderName)) {
            fullFileName = folderName + "/" + fullFileName;
        }

        String fileContentName = storeFileContent(fullFileName, content.getValue());
        return new UploadResultDto(folderName, fileContentName, DigestUtils.sha256Hex(content.getValue()));
    }

    private String storeFileContent(String fullFileName, byte[] content) {
        try {
            Path filePath = getFilePath(fullFileName);
            createDirsIfNotExist(filePath);
            Files.write(filePath, content, StandardOpenOption.CREATE);
            return FILE_PREFIX + fullFileName;
        } catch (IOException e) {
            log.error("Error storing file", e);
            throw new BusinessException("error.store.file", "File storage error");
        }
    }

    public Resource getFileFromFs(String fileContentUrl) {
        //fileName contains file name and details, the tenant subfolder is evaluated by getFilePath() function
        String simpleFileName = fileContentUrl;

        if (StringUtils.startsWith(fileContentUrl, FILE_PREFIX)) {
            simpleFileName = StringUtils.substringAfter(fileContentUrl, FILE_PREFIX);
        }

        Path filePath = getFilePath(simpleFileName);
        log.info("reading file {}", filePath);
        try {
            return new ByteArrayResource(Files.readAllBytes(filePath));
        } catch (Exception e) {
            log.error("Error reading file {}", filePath, e);
            throw new BusinessException("error.read.file", "File storage error");
        }

    }

    @Override
    public void delete(String contentUrl) {
        //fileName contains file name and details, tenant subfolder is evaluated by getFilePath() function
        String simpleFileName = StringUtils.substringAfter(contentUrl, FILE_PREFIX);
        Path filePath = getFilePath(simpleFileName);
        org.apache.commons.io.FileUtils.deleteQuietly(filePath.toFile());
    }

    private Path getFilePath(String fileName) {
        return Paths.get(applicationProperties.getObjectStorage().getFileRoot())
            .resolve(tenantContextHolder.getTenantKey().toLowerCase())
            .resolve(fileName)
            .normalize();
    }

    private String evaluateSubFolder() {
        String  subfolder = filePrefixNameFactory.evaluatePathSubfolder();
        if (StringUtils.isNotEmpty(subfolder)) {
            Matcher matcher = pattern.matcher(subfolder);
            boolean isValid = matcher.matches();
            if (!isValid) {
                throw new BusinessException("storage.file.subfolder.validation", "Subfolder " + subfolder + " is not matched for pattern \"" + FOLDER_NAME + "\"");
            }
        }
        return subfolder;
    }

    private String fileNameWithSubfolder(String fileName) {
        String fullFileName = getRandomMurMurFilePrefix(fileName);

        String subfolder = evaluateSubFolder();

        if (StringUtils.isNotEmpty(subfolder)) {
            return subfolder + "/" + fullFileName;
        }
        return fullFileName;
    }

    private String getRandomMurMurFilePrefix(String fileName) {
        final String randomPrefix =  Hashing.murmur3_128().hashString(UUID.randomUUID().toString(), StandardCharsets.UTF_8).toString();
        return randomPrefix + "-" + fileName;
    }

    private void createDirsIfNotExist(Path filePath) throws IOException {
        log.info("store path: {}", filePath);
        Path parentDir = filePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            log.debug("Created directory: {}", parentDir);
        }
    }

}
