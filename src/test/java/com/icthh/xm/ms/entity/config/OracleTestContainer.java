package com.icthh.xm.ms.entity.config;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Single shared Oracle container for the Oracle-specific integration tests. Startup costs about a minute,
 * so the instance is reused by every test class of that profile.
 */
public final class OracleTestContainer {

    /**
     * The user is named after the tenant on purpose: the tenant resolver switches the session schema to the
     * tenant key, and in Oracle a schema is a user, which the platform does not create for Oracle.
     */
    private static final OracleContainer INSTANCE = new OracleContainer(
        DockerImageName.parse("gvenzl/oracle-xe:18.4.0-slim"))
        .withUsername("test")
        .withPassword("test");

    static {
        // the JVM zone id may be unknown to the database timezone file, which fails every connection with ORA-01882
        System.setProperty("oracle.jdbc.timezoneAsRegion", "false");
    }

    private OracleTestContainer() {
    }

    public static OracleContainer getInstance() {
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
