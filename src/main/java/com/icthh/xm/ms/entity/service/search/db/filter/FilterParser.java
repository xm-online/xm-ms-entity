package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Parses the JHipster-like filter grammar {@code <field>.<op>} from a JSON body (typed values)
 * or from GET query params (string values, types inferred).
 */
@Component
public class FilterParser {

    public static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "typeKey", "query", "includeSubTypes");
    private static final Pattern LONG = Pattern.compile("-?\\d+");
    private static final Pattern DOUBLE = Pattern.compile("-?\\d+\\.\\d+");

    public List<FilterCondition> parseBody(Map<String, Object> filter) {
        if (filter == null) {
            return List.of();
        }
        return filter.entrySet().stream()
            .map(entry -> toCondition(entry.getKey(), entry.getValue()))
            .toList();
    }

    public List<FilterCondition> parseQueryParams(Map<String, List<String>> queryParams) {
        if (queryParams == null) {
            return List.of();
        }
        return queryParams.entrySet().stream()
            .filter(entry -> isFilterParam(entry.getKey(), entry.getValue()))
            .map(entry -> toCondition(entry.getKey(), inferValue(entry.getKey(), entry.getValue().get(0))))
            .toList();
    }

    private static boolean isFilterParam(String key, List<String> values) {
        return !RESERVED_PARAMS.contains(key) && values != null && !values.isEmpty();
    }

    private static FilterCondition toCondition(String key, Object value) {
        ParsedKey parsed = parseKey(key);
        List<Object> values = parsed.operator.isMultiValue()
            ? assertCollection(key, parsed.operator, value)
            : List.of(assertScalar(key, parsed.operator, value));
        return new FilterCondition(parsed.field, parsed.operator, values);
    }

    /** GET values are text: split lists on comma and infer scalar types before the common path. */
    private static Object inferValue(String key, String raw) {
        if (parseKey(key).operator.isMultiValue()) {
            return Arrays.stream(raw.split(",")).map(String::trim).map(FilterParser::inferType).toList();
        }
        return inferType(raw);
    }

    private static List<Object> assertCollection(String key, FilterOperator operator, Object value) {
        if (!(value instanceof Collection<?> collection)) {
            throw new BusinessException(ERR_VALIDATION,
                "Filter operator " + operator.suffix() + " requires a list value: " + key);
        }
        return new ArrayList<>(collection);
    }

    private static Object assertScalar(String key, FilterOperator operator, Object value) {
        if (value instanceof Collection<?>) {
            throw new BusinessException(ERR_VALIDATION,
                "Filter operator " + operator.suffix() + " requires a scalar value: " + key);
        }
        if (operator == FilterOperator.SPECIFIED && !(value instanceof Boolean)) {
            throw new BusinessException(ERR_VALIDATION, "Filter operator specified requires true or false: " + key);
        }
        return value;
    }

    private static Object inferType(String raw) {
        if (LONG.matcher(raw).matches()) {
            return Long.valueOf(raw);
        }
        if (DOUBLE.matcher(raw).matches()) {
            return Double.valueOf(raw);
        }
        if ("true".equals(raw) || "false".equals(raw)) {
            return Boolean.valueOf(raw);
        }
        return raw;
    }

    private static ParsedKey parseKey(String key) {
        int dot = key.lastIndexOf('.');
        if (dot <= 0 || dot == key.length() - 1) {
            throw new BusinessException(ERR_VALIDATION, "Filter key must be <field>.<operator>: " + key);
        }
        FilterOperator operator = FilterOperator.bySuffix(key.substring(dot + 1))
            .orElseThrow(() -> new BusinessException(ERR_VALIDATION, "Unknown filter operator in key: " + key));
        return new ParsedKey(key.substring(0, dot), operator);
    }

    private record ParsedKey(String field, FilterOperator operator) {
    }
}
