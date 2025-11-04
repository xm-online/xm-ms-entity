package com.icthh.xm.ms.entity.domain;

import com.icthh.xm.ms.entity.util.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.function.Predicate;

import static com.icthh.xm.ms.entity.config.Constants.FILE_PREFIX;

/**
 * Enum for an attachment storage type
 */
public enum AttachmentStoreType {

    DB(StringUtils::isEmpty),
    FS(url -> StringUtils.isNotEmpty(url) && url.startsWith(FILE_PREFIX)),
    S3(url -> StringUtils.isNotEmpty(url) && url.contains(FileUtils.BUCKET_NAME_SEPARATOR));

    private final Predicate<String> matcher;

    AttachmentStoreType(Predicate<String> matcher) {
        this.matcher = matcher;
    }

    public boolean matches(String contentUrl) {
        return matcher.test(contentUrl);
    }

    public static AttachmentStoreType byContentUrl(String contentUrl) {
        return Arrays.stream(values())
            .filter(type -> type.matches(contentUrl))
            .findFirst()
            .orElse(DB); // Fallback to DB
    }

}
