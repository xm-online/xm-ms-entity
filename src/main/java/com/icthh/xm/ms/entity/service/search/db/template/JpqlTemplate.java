package com.icthh.xm.ms.entity.service.search.db.template;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;

@Data
public class JpqlTemplate {

    private static final Pattern PARAM = Pattern.compile("(?<![:\\w]):([A-Za-z_][A-Za-z0-9_]*)");

    private String key;
    private JpqlTemplateType type = JpqlTemplateType.ENTITY;
    private String query;
    private String countQuery;
    /** param name → number | string | boolean | instant | list (used to coerce GET string values). */
    private Map<String, String> params = new HashMap<>();

    public Set<String> paramNames() {
        Set<String> names = new LinkedHashSet<>();
        collect(query, names);
        collect(countQuery, names);
        return names;
    }

    private static void collect(String text, Set<String> names) {
        if (text == null) {
            return;
        }
        Matcher m = PARAM.matcher(text);
        while (m.find()) {
            names.add(m.group(1));
        }
    }
}
