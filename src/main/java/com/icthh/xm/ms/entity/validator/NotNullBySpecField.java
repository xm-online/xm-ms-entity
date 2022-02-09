package com.icthh.xm.ms.entity.validator;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum NotNullBySpecField {

    KEY("key", XmEntity::getKey, TypeSpec::getIsKeyRequired),
    NAME("name", XmEntity::getName, TypeSpec::getIsNameRequired);

    String fieldName;
    Function<XmEntity, Object> valueExtractor;
    Function<TypeSpec, Boolean> isRequiredExtractor;

}
