package com.icthh.xm.ms.entity.config;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Single shared Postgres container for all Postgres-backed integration tests, so that they share one Spring context
 * (same JDBC URL) and Liquibase runs once.
 */
public final class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:14.17")
        .withDatabaseName("entity")
        .withUsername("sa")
        .withPassword("sa");

    private PostgresTestContainer() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }
}
