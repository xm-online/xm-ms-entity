package com.icthh.xm.ms.entity.repository.backend;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.service.storage.file.FileStorageService;
import com.icthh.xm.ms.entity.util.ImageResizeUtil;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRepository {

    private final ApplicationProperties applicationProperties;
    private final FileStorageService minioFileStorage;
    private final AmazonS3Template amazonS3Template;

    @SneakyThrows
    public String store(MultipartFile file, @CheckForNull Integer size) {
        return store(file.getInputStream(), size, file.getContentType(), file.getOriginalFilename());
    }

    @SneakyThrows
    public String store(HttpEntity<Resource> httpEntity, @CheckForNull Integer size) {
        return store(
            httpEntity.getBody().getInputStream(),
            size,
            httpEntity.getHeaders().getContentType().getType(),
            XmHttpEntityUtils.getFileName(httpEntity.getHeaders())
        );
    }

    private String store(
        InputStream stream, @CheckForNull Integer size, @CheckForNull String contentType, String fileName
    ) {
        try {
            if (nonNull(size) && nonNull(contentType) && "image".equals(contentType.substring(0, 5))) {
                stream = ImageResizeUtil.resize(stream, size);
            }

            String filename = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(fileName);

            persist(filename, stream);

            closeQuietly(stream);

            return generatePrefix() + filename;
        } catch (Exception e) {
            log.error("Error storing file", e);
            throw new BusinessException("Error storing file");
        }
    }

    private String generatePrefix() {
        switch (applicationProperties.getFileStorage().getActiveType()) {
            case AWS:
                return String.format(
                    applicationProperties.getAmazon().getAws().getTemplate(),
                    applicationProperties.getAmazon().getS3().getBucket()
                );
            case MINIO:
                return String.format(
                    applicationProperties.getFileStorage().getMinio().getTemplate(),
                    applicationProperties.getFileStorage().getMinio().getBucket()
                );
            default:
                throw new IllegalArgumentException("Invalid storage type");
        }
    }

    @SneakyThrows
    private void persist(String filename, InputStream stream) {
        switch (applicationProperties.getFileStorage().getActiveType()) {
            case AWS:
                amazonS3Template.save(filename, stream);
                break;
            case MINIO:
                minioFileStorage.save(filename, stream);
                break;
            default:
                throw new IllegalArgumentException("Invalid storage type");
        }
    }

    private void closeQuietly(final InputStream stream) {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException ignore) {
            log.warn("Close stream fail: {}", ignore.getMessage());
        }
    }
}
