package com.icthh.xm.ms.entity.config.jsonb;

import io.github.jhipster.domain.util.FixedPostgreSQL95Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StringType;

public class CustomPostgreSQL95Dialect extends FixedPostgreSQL95Dialect {

    public static final String JSON_QUERY_TEMPLATE = "jsonb_path_query_first(?1, ?2::jsonpath)";
    public static final String JSON_QUERY = "json_query";

    public static final String JSON_EXTRACT_PATH = "jsonb_extract_path_text";
    public static final String JSON_EXTRACT_PATH_TEMPLATE = "jsonb_to_string";

    public static final String TO_JSON_B = "to_json_b";
    public static final String TO_JSON_B_TEMPLATE = "to_jsonb(?1)";

    public CustomPostgreSQL95Dialect() {
        super();
        registerFunction(JSON_QUERY, new SQLFunctionTemplate(StringType.INSTANCE, JSON_QUERY_TEMPLATE));
        registerFunction(TO_JSON_B, new SQLFunctionTemplate(StringType.INSTANCE, TO_JSON_B_TEMPLATE));
        registerFunction(JSON_EXTRACT_PATH_TEMPLATE, new StandardSQLFunction(JSON_EXTRACT_PATH, StringType.INSTANCE));
    }

}
