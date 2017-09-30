package com.icthh.xm.ms.entity.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;

/**
 * The {@link Asserts} class.
 */
@UtilityClass
public final class Asserts {

    public static String requireNonBlank(String str) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException();
        }
        return str;
    }

    public static String requireNonBlank(String str, String message) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    public static String requireNonBlank(String str, Supplier<String> supplier) {
        if (StringUtils.isBlank(str)) {
            throw new IllegalArgumentException(supplier.get());
        }
        return str;
    }

}
