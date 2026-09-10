package com.icthh.xm.ms.entity.service.search.db.template;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/** One entry of jpql-templates.yml. Named params are read from the parsed query at execution time. */
@Data
public class JpqlTemplate {

    private String key;
    private JpqlTemplateType type = JpqlTemplateType.ENTITY;
    private String query;
    private String countQuery;
    /** param name → number | string | boolean | instant | list (converts GET string values). */
    private Map<String, String> params = new HashMap<>();
}
