package com.icthh.xm.ms.entity.config.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides command for schema change for particular database.
 */
@Slf4j
@Component
public class SchemaChangeResolver {

    private static final String DEFAULT_COMMAND = "USE %s";

    private static final Map<String, String> DBCOMMANDS = new HashMap<>();

    static {
        DBCOMMANDS.put("POSTGRESQL", "SET search_path TO %s");
        DBCOMMANDS.put("ORACLE", "ALTER SESSION SET CURRENT_SCHEMA = %s");
        DBCOMMANDS.put("H2", DEFAULT_COMMAND);
    }

    private String dbSchemaChangeCommand;

    public SchemaChangeResolver(Environment env) {
        String db = env.getProperty("spring.jpa.database");
        this.dbSchemaChangeCommand = DBCOMMANDS.getOrDefault(db, DEFAULT_COMMAND);
        log.info("Database {} will use command '{}' for schema changing", db, dbSchemaChangeCommand);
    }

    public String getSchemaSwitchCommand() {
        return this.dbSchemaChangeCommand;
    }

}
