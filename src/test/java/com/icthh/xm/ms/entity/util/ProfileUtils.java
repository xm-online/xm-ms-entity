package com.icthh.xm.ms.entity.util;

import com.icthh.xm.ms.entity.domain.Profile;

import java.util.UUID;
import java.util.function.Consumer;

public class ProfileUtils {

    public static Long TEST_ID = 0L;
    public static String TEST_USER_KEY = UUID.randomUUID().toString();

    public static Consumer<Profile> defaultProfile() {
        return e -> {
            e.setId(TEST_ID);
            e.setUserKey(TEST_USER_KEY);
        };
    }

    public static Profile newProfile() {
        return newProfile(defaultProfile());
    }

    public static Profile newProfile(Consumer<Profile> profile) {
        Profile p = new Profile();
        profile.accept(p);
        return p;
    }
}
