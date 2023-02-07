package com.icthh.xm.ms.entity.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@UtilityClass
public class FileUtils {

    private static final String CONTENT_DISPOSITION_FORMAT = "attachment; filename=\"%s\"";

    public static final String FILE_NAME_SEPARATOR = "::";

    public static Pair<String, String> getS3BucketNameKey(String contentUrl) {
        String[] split = contentUrl.split(FILE_NAME_SEPARATOR);
        if (split.length != 2) {
            throw new IllegalArgumentException("Invalid format for url = " + contentUrl);
        }

        return Pair.of(split[0], split[1]);
    }

    public static String getContentDisposition(String fileName) {
        return String.format(CONTENT_DISPOSITION_FORMAT, fileName);
    }
}
