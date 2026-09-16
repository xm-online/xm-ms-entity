package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

/**
 * A path into the {@code data} jsonb column: {@code data.<segment>(.<segment>)*}.
 * Single place where filter and sort agree on what a valid data path is and how it maps to a SQL/JSON path.
 */
@UtilityClass
public class DataPath {

    public static final String PREFIX = "data.";
    private static final Pattern VALID = Pattern.compile("^data(\\.[A-Za-z0-9_]+)+$");

    public static boolean isDataPath(String field) {
        return field != null && field.startsWith(PREFIX);
    }

    public static boolean isValid(String field) {
        return field != null && VALID.matcher(field).matches();
    }

    /** {@code data.a.b} → {@code $.a.b}. */
    public static String toJsonPath(String field) {
        assertValid(field);
        return "$." + field.substring(PREFIX.length());
    }

    public static void assertValid(String field) {
        if (!isValid(field)) {
            throw new BusinessException(ERR_VALIDATION, "Invalid data path: " + field);
        }
    }
}
