package com.icthh.xm.ms.entity.validator;

import com.icthh.xm.ms.entity.domain.EntityBaseFields;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum NotNullBySpecField {

    KEY("key", EntityBaseFields::getKey, TypeSpec::getIsKeyRequired),
    NAME("name", EntityBaseFields::getName, TypeSpec::getIsNameRequired);

    final String fieldName;
    final Function<EntityBaseFields, Object> valueExtractor;
    final Function<TypeSpec, Boolean> isRequiredExtractor;

}
