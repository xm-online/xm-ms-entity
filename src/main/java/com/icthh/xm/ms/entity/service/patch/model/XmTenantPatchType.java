package com.icthh.xm.ms.entity.service.patch.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum XmTenantPatchType {
    CREATE_INDEX(GeneralCreateIndexPatch.class),
    CREATE_JSONPATH_INDEX(JsonPathCreateIndexPatch.class),
    CREATE_SEQUENCE(CreateSequencePatch.class),
    DROP_INDEX(DropIndexPatch.class),
    DROP_SEQUENCE(DropSequencePatch.class);

    private final Class<? extends XmTenantChangeSet> implementationClass;
}
