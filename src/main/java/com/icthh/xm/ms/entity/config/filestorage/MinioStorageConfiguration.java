package com.icthh.xm.ms.entity.config.filestorage;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioStorageConfiguration {

    @Bean
    public MinioClient minioClient(ApplicationProperties applicationProperties) {
        ApplicationProperties.FileStorage.Minio minio = applicationProperties.getFileStorage().getMinio();
        return MinioClient.builder()
            .endpoint(minio.getHostname(), minio.getPort(), false)
            .credentials(minio.getAccessKey(), minio.getSecretKey())
            .build();
    }
}
