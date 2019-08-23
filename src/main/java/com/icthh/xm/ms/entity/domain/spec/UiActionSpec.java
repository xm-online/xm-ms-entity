package com.icthh.xm.ms.entity.domain.spec;

import com.google.common.collect.Sets;

import java.util.Set;

public enum UiActionSpec {
    CREATE, UPDATE, EXECUTE, DELETE, READ;

    public static Set<UiActionSpec> readOnly() {
        return Sets.immutableEnumSet(READ);
    }

    public static Set<UiActionSpec> all() {
        return Sets.immutableEnumSet(READ, CREATE, UPDATE, DELETE);
    }

}
