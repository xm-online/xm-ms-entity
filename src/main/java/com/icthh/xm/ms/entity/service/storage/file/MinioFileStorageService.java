package com.icthh.xm.ms.entity.service.storage.file;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileStorageService implements FileStorageService {

    private final ApplicationProperties properties;
    private final MinioClient minioClient;

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
            .bucket(properties.getFileStorage().getMinio().getBucket())
            .build();
    }

    private MakeBucketArgs configBucket() {
        return MakeBucketArgs.builder()
            .bucket(properties.getFileStorage().getMinio().getBucket())
            .build();
    }

    private PutObjectArgs toObjectSize(String fileName, InputStream inputStream) {
        return PutObjectArgs.builder()
            .bucket(properties.getFileStorage().getMinio().getBucket())
            .object(fileName)
            .stream(inputStream, -1, 10485760)
            .build();
    }
}
