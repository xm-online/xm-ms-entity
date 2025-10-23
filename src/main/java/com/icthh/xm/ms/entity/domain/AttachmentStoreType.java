package com.icthh.xm.ms.entity.domain;

import com.icthh.xm.ms.entity.util.FileUtils;
import org.apache.commons.lang3.StringUtils;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;

/**
 * Enum for attachment storage type
 */
public enum AttachmentStoreType {

    DB, S3, FS;

    public static AttachmentStoreType byContentUrl(String contentUrl) {
        if (StringUtils.isEmpty(contentUrl)) {
            return AttachmentStoreType.DB;
        }
        if (contentUrl.startsWith(FILE_PREFIX)) {
            return AttachmentStoreType.FS;
        }
        if (contentUrl.contains(FileUtils.BUCKET_NAME_SEPARATOR)) {
            return AttachmentStoreType.S3;
        }
        return AttachmentStoreType.DB;
    }

}
