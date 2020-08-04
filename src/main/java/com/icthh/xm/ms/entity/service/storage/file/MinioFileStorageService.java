package com.icthh.xm.ms.entity.service.storage.file;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
public class MinioFileStorageService implements FileStorageService {

    private final String bucket;
    private final MinioClient minioClient;

    public MinioFileStorageService(
        @Value("${application.file-storage.minio.bucket}") String bucket,
        MinioClient minioClient
    ) {
        this.bucket = bucket;
        this.minioClient = minioClient;
    }

    @Override
    @SneakyThrows
    public void save(String fileName, InputStream inputStream) {
        boolean bucketExists = minioClient.bucketExists(bucketExists());

        if (!bucketExists) {
            minioClient.makeBucket(configBucket());
        }

        minioClient.putObject(toObjectSize(fileName, inputStream));
    }

    private BucketExistsArgs bucketExists() {
        return BucketExistsArgs.builder()
            .bucket(bucket)
            .build();
    }

    private MakeBucketArgs configBucket() {
        return MakeBucketArgs.builder()
            .bucket(bucket)
            .build();
    }

    private PutObjectArgs toObjectSize(String fileName, InputStream inputStream) {
        return PutObjectArgs.builder()
            .bucket(bucket)
            .object(fileName)
            .stream(inputStream, -1, 10485760)
            .build();
    }
}
