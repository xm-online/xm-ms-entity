package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import java.util.ArrayList;
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
        List<FilterCondition> result = new ArrayList<>();
        if (filter == null) {
            return result;
        }
        filter.forEach((key, value) -> {
            ParsedKey parsed = parseKey(key);
            List<Object> values;
            if (parsed.operator.isMultiValue()) {
                if (!(value instanceof Collection<?> collection)) {
                    throw new BusinessException(ERR_VALIDATION, "Filter operator " + parsed.operator.suffix()
                        + " requires a list value: " + key);
                }
                values = new ArrayList<>(collection);
            } else {
                values = List.of(requireScalar(key, parsed.operator, value));
            }
            result.add(new FilterCondition(parsed.field, parsed.operator, values));
        });
        return result;
    }

    public List<FilterCondition> parseQueryParams(Map<String, List<String>> queryParams) {
        List<FilterCondition> result = new ArrayList<>();
        if (queryParams == null) {
            return result;
        }
        queryParams.forEach((key, rawValues) -> {
            if (RESERVED_PARAMS.contains(key) || rawValues == null || rawValues.isEmpty()) {
                return;
            }
            ParsedKey parsed = parseKey(key);
            String raw = rawValues.get(0);
            List<Object> values = new ArrayList<>();
            if (parsed.operator.isMultiValue()) {
                for (String part : raw.split(",")) {
                    values.add(inferType(part.trim()));
                }
            } else {
                values.add(requireScalar(key, parsed.operator, inferType(raw)));
            }
            result.add(new FilterCondition(parsed.field, parsed.operator, values));
        });
        return result;
    }

    private static Object requireScalar(String key, FilterOperator operator, Object value) {
        if (value instanceof Collection<?>) {
            throw new BusinessException(ERR_VALIDATION, "Filter operator " + operator.suffix()
                + " requires a scalar value: " + key);
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
        String field = key.substring(0, dot);
        FilterOperator operator = FilterOperator.bySuffix(key.substring(dot + 1))
            .orElseThrow(() -> new BusinessException(ERR_VALIDATION, "Unknown filter operator in key: " + key));
        return new ParsedKey(field, operator);
    }

    private record ParsedKey(String field, FilterOperator operator) {
    }
}
