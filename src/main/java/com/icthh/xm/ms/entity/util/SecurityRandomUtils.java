package com.icthh.xm.ms.entity.util;

import com.google.common.collect.ImmutableList;
import java.security.SecureRandom;

public class SecurityRandomUtils {

    private static final ImmutableList<String> RANDOM_STRINGS = ImmutableList.of(
        "other", "another", "different", "second", "new", "fresh", "any", "whatever"
    );

    public static int generateRandomInt(int maxValue) {
        if (maxValue <= 0) {
            return 0;
        }
        return new SecureRandom().nextInt(maxValue);
    }

    public static int generateRandomInt() {
        return generateRandomInt(Integer.MAX_VALUE);
    }

    public static boolean generateRandomBoolean() {
        return new SecureRandom().nextBoolean();
    }

    public static String generateRandomString() {
        return RANDOM_STRINGS.get(generateRandomInt(RANDOM_STRINGS.size()));
    }

}
