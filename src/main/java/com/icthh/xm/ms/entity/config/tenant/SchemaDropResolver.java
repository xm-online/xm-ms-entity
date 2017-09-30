package com.icthh.xm.ms.entity.config.tenant;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Provides command for schema drop for particular database.
 */
@Slf4j
@Component
public class SchemaDropResolver {
    private static final String DDL_DROP_SCHEMA = "DROP SCHEMA IF EXISTS %s ";
    private static final Map<String, String> DB_COMMANDS = new HashMap<>();

    static {
        DB_COMMANDS.put("POSTGRESQL", DDL_DROP_SCHEMA + "CASCADE");
        DB_COMMANDS.put("H2", DDL_DROP_SCHEMA);
    }

    private String dbSchemaDropCommand;

    /**
     * Constructor.
     * @param env environment
     */
    public SchemaDropResolver(Environment env) {
        String db = env.getProperty("spring.jpa.database");
        this.dbSchemaDropCommand = DB_COMMANDS.getOrDefault(db, DDL_DROP_SCHEMA);
        log.info("Database {} will use command '{}' for schema dropping", db, dbSchemaDropCommand);
    }

    public String getSchemaDropCommand() {
        return this.dbSchemaDropCommand;
    }

}
