package com.icthh.xm.ms.entity.repository.backend;

import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.amazon.AmazonS3Template;
import com.icthh.xm.ms.entity.util.ImageResizeUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Component
public class StorageRepository {

    private final ApplicationProperties applicationProperties;
    private final AmazonS3Template amazonS3Template;

    public String store(MultipartFile file, Integer size) {
        try {
            InputStream stream = file.getInputStream();
            if (size != null && file.getContentType() != null && "image".equals(file.getContentType().substring(0, 5))) {
                stream = ImageResizeUtil.resize(stream, size);
            }
            String filename =
                UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(file.getOriginalFilename());
            amazonS3Template.save(filename, stream);
            IOUtils.closeQuietly(stream);

            String prefix = String.format(applicationProperties.getAmazon().getAws().getTemplate(),
                applicationProperties.getAmazon().getS3().getBucket());
            return prefix + filename;
        } catch (IOException e) {
            log.error("Error storing file", e);
            throw new BusinessException("Error storing file");
        }
    }

}
