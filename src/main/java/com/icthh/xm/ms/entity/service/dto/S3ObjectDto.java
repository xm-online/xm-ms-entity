package com.icthh.xm.ms.entity.service.dto;

import com.amazonaws.services.s3.model.S3Object;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.io.InputStream;

@Data
@ToString
@AllArgsConstructor
public class S3ObjectDto {

    private String key;
    private String bucketName;
    private String eTag;
    private String contentType;
    private Long contentLength;

    private InputStream objectContent;

    public static S3ObjectDto from(S3Object object) {
        String eTag = object.getObjectMetadata().getETag();
        String contentType = object.getObjectMetadata().getContentType();
        Long contentLength = object.getObjectMetadata().getContentLength();
        return new S3ObjectDto(object.getKey(), object.getBucketName(), eTag, contentType, contentLength, object.getObjectContent());
    }
}
