package com.icthh.xm.ms.entity.service.dto;

import com.amazonaws.services.s3.transfer.model.UploadResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class UploadResultDto {
    private String bucketName;
    private String key;
    private String eTag;

    public static UploadResultDto from(UploadResult uploadResult) {
        return new UploadResultDto(uploadResult.getBucketName(), uploadResult.getKey(), uploadResult.getETag());
    }
}
