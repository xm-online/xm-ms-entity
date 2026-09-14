package com.icthh.xm.ms.entity.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import lombok.SneakyThrows;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Single shared Oracle container for the Oracle-specific integration tests. Startup costs about a minute,
 * so the instance is reused by every test class of that profile.
 */
public final class OracleTestContainer {

    /**
     * The user is named after the tenant on purpose: the tenant resolver switches the session schema to the
     * tenant key, and in Oracle a schema is a user, so the container user must be TEST.
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
            createLegacySequences();
        }
        return INSTANCE;
    }

    /**
     * Hibernate creates the sequences of the mapping, but {@code hibernate_sequence} comes from the initial
     * Liquibase changelog, which this profile does not run.
     */
    @SneakyThrows
    private static void createLegacySequences() {
        try (Connection connection = DriverManager.getConnection(
            INSTANCE.getJdbcUrl(), INSTANCE.getUsername(), INSTANCE.getPassword());
             Statement statement = connection.createStatement()) {
            for (String sequence : List.of("hibernate_sequence", "sequence_generator")) {
                statement.execute("""
                    declare
                        found number;
                    begin
                        select count(*) into found from user_sequences where sequence_name = upper('%s');
                        if found = 0 then
                            execute immediate 'create sequence %s start with 1000 increment by 50';
                        end if;
                    end;""".formatted(sequence, sequence));
            }
        }
    }
}
