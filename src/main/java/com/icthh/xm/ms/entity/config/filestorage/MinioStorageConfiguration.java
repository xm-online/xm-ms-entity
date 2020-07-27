package com.icthh.xm.ms.entity.config.filestorage;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioStorageConfiguration {

    private ApplicationProperties applicationProperties;

    @Bean
    public MinioClient minioClient() {
        ApplicationProperties.FileStorage.Minio minio = applicationProperties.getFileStorage().getMinio();
        return MinioClient.builder()
            .endpoint(minio.getUrl())
            .credentials(minio.getAccessKey(), minio.getSecretKey())
            .build();
    }
}
