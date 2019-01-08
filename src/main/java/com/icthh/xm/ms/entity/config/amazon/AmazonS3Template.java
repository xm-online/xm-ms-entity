package com.icthh.xm.ms.entity.config.amazon;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

@Slf4j
@RequiredArgsConstructor
public class AmazonS3Template {

    private final String bucket;
    private final String endpoint;
    private final String region;
    private final String accessKeyId;
    private final String accessKeySecret;

    private AmazonS3 amazonS3;
    private TransferManager transferManager;

    /**
     * Save a file using authenticated session credentials.
     *
     * @param key is the name of the file to save in the bucket
     * @param inputStream is the file that will be saved
     */
    public void save(String key, InputStream inputStream) throws IOException {
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
     * Get a file using the authenticated session credentials.
     *
     * @param key is the key of the file in the bucket that should be retrieved
     * @return an instance of {@link S3Object} containing the file from S3
     */
    public S3Object get(String key) {
        return getAmazonS3Client().getObject(bucket, key);
    }

    public S3Object getWithBucket(String bucket, String key) {
        return getAmazonS3Client().getObject(bucket, key);
    }

    /**
     * Gets an Amazon S3 client from basic session credentials.
     *
     * @return an authenticated Amazon S3 amazonS3
     */
    public AmazonS3 getAmazonS3Client() {
        if (amazonS3 == null) {
            amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new EndpointConfiguration(endpoint, region))
                .withClientConfiguration(new ClientConfiguration().withProtocol(Protocol.HTTP))
                .withCredentials(
                    new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, accessKeySecret)))
                .build();
        }
        return amazonS3;
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

}
