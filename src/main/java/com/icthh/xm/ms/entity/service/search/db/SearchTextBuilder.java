package com.icthh.xm.ms.entity.service.search.db;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Builds the denormalized text stored in xm_entity.search_text. Deterministic: name, description, configured data fields. */
@Slf4j
@Component
public class SearchTextBuilder {

    private static final String DATA_PREFIX = "data.";
    private static final String SEPARATOR = "\n";

    public String build(TypeSpec spec, XmEntity entity) {
        if (spec == null || !Boolean.TRUE.equals(spec.getFullTextSearch())) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        addIfNotBlank(parts, entity.getName());
        addIfNotBlank(parts, entity.getDescription());
        List<String> fields = spec.getFullTextSearchDataFields() == null ? List.of() : spec.getFullTextSearchDataFields();
        for (String field : fields) {
            String path = field.startsWith(DATA_PREFIX) ? field.substring(DATA_PREFIX.length()) : field;
            addIfNotBlank(parts, scalarText(resolve(entity.getData(), path), field));
        }
        return String.join(SEPARATOR, parts);
    }

    private static Object resolve(Map<String, Object> data, String path) {
        Object current = data;
        for (String segment : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }

    private static String scalarText(Object value, String field) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?>) {
            log.debug("fullTextSearchDataFields entry {} points to an object, skipped", field);
            return null;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                .filter(v -> v != null && !(v instanceof Map<?, ?>) && !(v instanceof Collection<?>))
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        }
        return String.valueOf(value);
    }

    private static void addIfNotBlank(List<String> parts, String value) {
        if (StringUtils.isNotBlank(value)) {
            parts.add(value);
        }
    }
}
