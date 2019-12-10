package com.icthh.xm.ms.entity.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@UtilityClass
public class ResourceUtils {

    private static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");

    public static String getResourceAsStr(String location) {
        Resource res = new ClassPathResource(location);
        try {
            try (InputStream is = res.getInputStream()) {
                return IOUtils.toString(is, CHARSET_UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
