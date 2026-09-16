package com.icthh.xm.ms.entity.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Records every SQL statement Hibernate sends, so tests can assert on the exact shape of the generated SQL
 * (for example that a jsonb expression matches the one an expression index is built on). Registered through
 * {@code hibernate.session_factory.statement_inspector} in the database test profiles.
 */
public class SqlCaptureStatementInspector implements StatementInspector {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql);
        return sql;
    }

    public static void clear() {
        STATEMENTS.clear();
    }

    /** The most recent {@code select} that is not a count query. */
    public static String lastSelect() {
        return STATEMENTS.reversed().stream()
            .filter(s -> s.startsWith("select") && !s.startsWith("select count("))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("no select captured"));
    }
}
