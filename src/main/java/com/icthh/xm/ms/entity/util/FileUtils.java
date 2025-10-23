package com.icthh.xm.ms.entity.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;

@UtilityClass
public class FileUtils {

   public static final String BUCKET_NAME_SEPARATOR = "::";

    public static Pair<String, String> getS3BucketNameKey(String contentUrl) {
        String[] split = contentUrl.split(BUCKET_NAME_SEPARATOR);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid format for url = " + contentUrl);
        }

        return Pair.of(split[0], split[1]);
    }

    public static String getContentDisposition(String fileName) {
        return ContentDisposition.builder("attachment")
            .filename(fileName, StandardCharsets.UTF_8)
            .build()
            .toString();
    }
}
