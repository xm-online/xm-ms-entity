package com.icthh.xm.ms.entity.config.amazon;

import com.amazonaws.AmazonClientException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.domain.Attachment;
import com.icthh.xm.ms.entity.domain.Content;
import com.icthh.xm.ms.entity.service.dto.S3ObjectDto;
import com.icthh.xm.ms.entity.service.dto.UploadResultDto;
import com.icthh.xm.ms.entity.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor=@__(@Lazy))
public class AmazonS3Template {

    private static final String FILE_NAME_ATTRIBUTE = "fileName";

    private final ApplicationProperties applicationProperties;
    private final AmazonS3ClientFactory amazonS3ClientFactory;
    private final AmazonS3BucketNameFactory bucketNameFactory;

    private TransferManager transferManager;

    /**
     * Save a file using authenticated session credentials.
     *
     * @param key is the name of the file to save in the bucket
     * @param inputStream is the file that will be saved
     */
    public void save(String key, InputStream inputStream) throws IOException {
        String bucket = applicationProperties.getAmazon().getS3().getBucket();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(URLConnection.guessContentTypeFromStream(inputStream));

        PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, metadata);
        request.setCannedAcl(CannedAccessControlList.PublicRead);
        request.getRequestClientOptions().setReadLimit(Integer.MAX_VALUE);

        Upload upload = getTransferManager().upload(request);
        try {
            upload.waitForUploadResult();
        } catch (AmazonClientException ex) {
            throw new IOException(ex);
        } catch (InterruptedException ex) {
            // reset interrupted status
            Thread.currentThread().interrupt();
            // continue interrupt
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Save a file using authenticated session credentials.
     *
     * @param bucket      os the name of the s3 bucket
     * @param key         is the name of the file to save in the bucket
     * @param inputStream is the file that will be saved
     */
    public String save(String bucket, String key, InputStream inputStream, Integer contentLength, String fileName) {
        UploadResult uploadResult = upload(bucket, key, inputStream, contentLength, fileName);
        return uploadResult.getKey();
    }

    @SneakyThrows
    public UploadResultDto save(String bucket, String key, Content content, String fileName) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getValue())) {
            UploadResult uploadResult = upload(bucket, key, inputStream, content.getValue().length, fileName);
            return UploadResultDto.from(uploadResult);
        }
    }

    @SneakyThrows
    private UploadResult upload(String bucket, String key, InputStream inputStream, Integer contentLength, String fileName) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(URLConnection.guessContentTypeFromStream(inputStream));
        metadata.setContentDisposition(FileUtils.getContentDisposition(fileName));
        metadata.addUserMetadata(FILE_NAME_ATTRIBUTE, fileName);
        metadata.setContentLength(contentLength);

        PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, metadata);
        request.getRequestClientOptions().setReadLimit(Integer.MAX_VALUE);

        Upload upload = getTransferManager().upload(request);
        try {
            return upload.waitForUploadResult();
        } catch (AmazonClientException ex) {
            throw new IOException(ex);
        } catch (InterruptedException ex) {
            // reset interrupted status
            Thread.currentThread().interrupt();
            // continue interrupt
            throw new IllegalStateException(ex);
        }
    }

    /**
     *
     * @param bucketPrefix - using for separate dev int prod env
     * @param bucket - bucket name
     * @return
     */
    public String createBucketIfNotExist(String bucketPrefix, String bucket) {
        String formattedBucketName = bucketNameFactory.prepareBucketName(bucketPrefix, bucket);
        String region = applicationProperties.getAmazon().getAws().getRegion();
        if (getAmazonS3Client().doesBucketExist(formattedBucketName)) {
            log.info("Bucket: {} exist", formattedBucketName);
        } else {
            log.info("Bucket: {} will be created in region {}", formattedBucketName, region);
            getAmazonS3Client().createBucket(new CreateBucketRequest(formattedBucketName, region));
        }

        return formattedBucketName;
    }

    @SneakyThrows
    public URL createExpirableLink(Attachment attachment, Long expireTimeMillis) {
        Pair<String, String> s3BucketNameKey = FileUtils.getS3BucketNameKey(attachment.getContentUrl());

        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += expireTimeMillis;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =  new GeneratePresignedUrlRequest(s3BucketNameKey.getKey(), s3BucketNameKey.getValue())
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration)
            .withResponseHeaders(createResponseHeaderOverrides(attachment));
        return getAmazonS3Client().generatePresignedUrl(generatePresignedUrlRequest);
    }

    @SneakyThrows
    public URL createExpirableLink(String bucket, String key, Long expireTimeMillis) {

        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += expireTimeMillis;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =  new GeneratePresignedUrlRequest(bucket, key)
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration);
        return getAmazonS3Client().generatePresignedUrl(generatePresignedUrlRequest);
    }

    private ResponseHeaderOverrides createResponseHeaderOverrides(Attachment attachment) {
        ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();
        responseHeaderOverrides.setContentDisposition(FileUtils.getContentDisposition(attachment.getName()));
        responseHeaderOverrides.setContentType(attachment.getValueContentType());
        return responseHeaderOverrides;
    }

    /**
     * Get a file using the authenticated session credentials.
     *
     * @param key is the key of the file in the bucket that should be retrieved
     * @return an instance of {@link S3Object} containing the file from S3
     */
    public S3Object get(String key) {
        String bucket = applicationProperties.getAmazon().getS3().getBucket();
        return getAmazonS3Client().getObject(bucket, key);
    }

    public void delete(String bucket, String key) {
        log.info("Delete from bucket = {}, key = {}", bucket, key);
        getAmazonS3Client().deleteObject(bucket, key);
    }

    public S3Object get(String bucket, String key) {
        return getAmazonS3Client().getObject(bucket, key);
    }

    public S3ObjectDto getS3Object(String bucket, String key) {
        S3Object s3Object = get(bucket, key);
        return S3ObjectDto.from(s3Object);
    }

    /**
     * Gets an Amazon transfer manager.
     *
     * @return a transfer manager
     */
    public TransferManager getTransferManager() {
        if (transferManager == null) {
            transferManager = TransferManagerBuilder.standard().withS3Client(getAmazonS3Client()).build();
        }
        return transferManager;
    }

    public AmazonS3 getAmazonS3Client() {
        return amazonS3ClientFactory.getAmazonS3();
    }

}
