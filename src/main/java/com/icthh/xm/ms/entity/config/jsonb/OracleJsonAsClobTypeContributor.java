package com.icthh.xm.ms.entity.config.jsonb;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;

/**
 * Stores {@code SqlTypes.JSON} as CLOB text on Oracle.
 *
 * <p>Hibernate maps JSON to BLOB on Oracle releases without the native JSON type, while the Liquibase changelog
 * creates {@code xm_entity.data} as CLOB. The BLOB binding then writes the document hex encoded, and every
 * Oracle JSON function ({@code json_value}, {@code json_query}) reads it as null, which silently empties every
 * jsonb filter and sort. Binding JSON as CLOB keeps the document readable both for the application and for SQL.
 */
@Slf4j
public class OracleJsonAsClobTypeContributor implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        Dialect dialect = serviceRegistry.getService(JdbcServices.class).getDialect();
        if (dialect instanceof OracleDialect) {
            typeContributions.getTypeConfiguration().getJdbcTypeRegistry()
                .addDescriptor(SqlTypes.JSON, ClobJdbcType.DEFAULT);
            log.info("Oracle detected: json columns are bound as CLOB text");
        }
    }
}
