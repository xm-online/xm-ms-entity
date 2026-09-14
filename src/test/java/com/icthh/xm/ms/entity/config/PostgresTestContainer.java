package com.icthh.xm.ms.entity.config;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Single shared Postgres container for all Postgres-backed integration tests, so that they share one Spring context
 * (same JDBC URL) and Liquibase runs once.
 */
public final class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:18")
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

    /**
     * Stops the container if it is running. Idempotent, so it may be called both when the last test of this
     * kind finishes and as a safety net when the test plan ends.
     */
    public static void stop() {
        if (INSTANCE.isRunning()) {
            INSTANCE.stop();
        }
    }
}
