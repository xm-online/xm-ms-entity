package com.icthh.xm.ms.entity.service.dto;

import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.icthh.xm.ms.entity.util.FileUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;

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

    public String toXmContentName() {
        if (StringUtils.isNotEmpty(key) && key.startsWith(FILE_PREFIX)) {
            return key;
        }
        return bucketName + FileUtils.BUCKET_NAME_SEPARATOR + key;
    }
}
