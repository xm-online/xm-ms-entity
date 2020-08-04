package com.icthh.xm.ms.entity.config.filestorage;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioStorageConfiguration {

    private final String accessKey;
    private final String secretKey;
    private final String hostName;
    private final int port;

    public MinioStorageConfiguration(
        @Value("${application.file-storage.minio.access-key}") String accessKey,
        @Value("${application.file-storage.minio.secret-key}") String secretKey,
        @Value("${application.file-storage.minio.host-name}") String hostName,
        @Value("${application.file-storage.minio.port}") int port
    ) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.hostName = hostName;
        this.port = port;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(hostName, port, false)
            .credentials(accessKey, secretKey)
            .build();
    }
}
