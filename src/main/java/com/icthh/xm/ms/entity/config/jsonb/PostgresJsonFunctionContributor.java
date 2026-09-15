package com.icthh.xm.ms.entity.config.jsonb;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.StandardBasicTypes;

/**
 * PostgreSQL jsonb extraction in the exact form the tenant db patches index.
 *
 * <p>{@value #JSON_QUERY}{@code (data, '$.path')} renders {@code jsonb_path_query_first(data, '$.path'::jsonpath)},
 * the expression that the {@code CREATE_JSONPATH_INDEX} patch of xm-ms-ee-entity builds its btree indexes on.
 * PostgreSQL uses an expression index only when the query contains the identical expression, so every
 * {@code data.*} filter and sort goes through this function.
 *
 * <p>xm-commons registers the same pattern under the name {@code json_query}, but Hibernate 7 ships a built-in
 * {@code json_query} for PostgreSQL (a scalar subquery over {@code jsonb_path_query}) and registers it after the
 * custom dialect, so that name now renders the subquery, which no index can serve. A dedicated name avoids the
 * clash.
 */
public class PostgresJsonFunctionContributor implements FunctionContributor {

    public static final String JSON_QUERY = "xm_json_query";

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        if (functionContributions.getDialect() instanceof PostgreSQLDialect) {
            functionContributions.getFunctionRegistry().registerPattern(JSON_QUERY,
                "jsonb_path_query_first(?1, ?2::jsonpath)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.STRING));
        }
    }
}
