package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import lombok.experimental.UtilityClass;

/** Strict boolean parsing shared by filters and template params: only {@code true} / {@code false}. */
@UtilityClass
public class BooleanValues {

    public static Boolean parse(String name, String text) {
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.valueOf(text);
        }
        throw new BusinessException(ERR_VALIDATION, "Invalid boolean value for " + name + ": " + text);
    }
}
