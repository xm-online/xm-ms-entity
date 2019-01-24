package com.icthh.xm.ms.entity.repository.backend;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class StorageRepository {

    private final ApplicationProperties applicationProperties;
    private final AmazonS3Template amazonS3Template;

    @SneakyThrows
    public String store(MultipartFile file, Integer size) {
        return store(file.getInputStream(), size, file.getContentType(), file.getOriginalFilename());
    }

    @SneakyThrows
    public String store(HttpEntity<Resource> httpEntity, Integer size) {
        return store(httpEntity.getBody().getInputStream(), size,
            httpEntity.getHeaders().getContentType().getType(), XmHttpEntityUtils.getFileName(httpEntity.getHeaders()));
    }

    private String store(InputStream stream, Integer size, String contentType, String name) {
        try {
            if (size != null && contentType != null && "image".equals(contentType.substring(0, 5))) {
                stream = ImageResizeUtil.resize(stream, size);
            }
            String filename = UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(name);
            amazonS3Template.save(filename, stream);

            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException ignore) {
            }

            String prefix = String.format(applicationProperties.getAmazon().getAws().getTemplate(),
                applicationProperties.getAmazon().getS3().getBucket());
            return prefix + filename;
        } catch (IOException e) {
            log.error("Error storing file", e);
            throw new BusinessException("Error storing file");
        }
    }

}
