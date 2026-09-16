package com.icthh.xm.ms.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.config.PostgresTestContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Runs the changelog the way production does: first on {@code spring.liquibase.default-schema} (XM), then on every
 * tenant schema. pg_trgm is installed once per database, so the first run must not capture it into its own schema:
 * the other tenants would then not see {@code gin_trgm_ops} and could not create the trigram index.
 */
public class SearchTextTrgmIndexMigrationIntTest extends AbstractPostgresIntTest {

    private static final String DATABASE = "search_text_migration";
    private static final List<String> SCHEMAS = List.of("xm", "t2");

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    public void freshDatabase() throws Exception {
        PostgreSQLContainer<?> container = PostgresTestContainer.getInstance();
        recreateDatabase(container);
        dataSource = new DriverManagerDataSource(
            "jdbc:postgresql://" + container.getHost() + ":" + container.getMappedPort(5432) + "/" + DATABASE,
            container.getUsername(), container.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        SCHEMAS.forEach(schema -> jdbc.execute("CREATE SCHEMA " + schema));
    }

    @Test
    public void installsExtensionInPublicAndIndexesEveryTenantSchema() throws Exception {
        migrateAllSchemas();

        assertThat(extensionSchema()).isEqualTo("public");
        assertThat(indexedSchemas()).containsExactly("t2", "xm");
    }

    /** The prod entity database has no public schema: the changelog creates it rather than losing the index. */
    @Test
    public void createsPublicSchemaWhenMissing() throws Exception {
        jdbc.execute("DROP SCHEMA public");

        migrateAllSchemas();

        assertThat(extensionSchema()).isEqualTo("public");
        assertThat(indexedSchemas()).containsExactly("t2", "xm");
    }

    /**
     * Databases migrated by the first version of the changelog have pg_trgm in the first migrated schema. The app
     * must still start, and the index appears on the next start after the extension is moved to public.
     */
    @Test
    public void skipsIndexUntilExtensionIsMovedToPublic() throws Exception {
        jdbc.execute("CREATE EXTENSION pg_trgm SCHEMA xm");

        migrateAllSchemas();
        assertThat(extensionSchema()).isEqualTo("xm");
        assertThat(indexedSchemas()).isEmpty();

        jdbc.execute("ALTER EXTENSION pg_trgm SET SCHEMA public");
        migrateAllSchemas();
        assertThat(indexedSchemas()).containsExactly("t2", "xm");
    }

    private void migrateAllSchemas() throws Exception {
        for (String schema : SCHEMAS) {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:config/liquibase/master.xml");
            liquibase.setContexts("prod");
            liquibase.setDefaultSchema(schema);
            liquibase.setResourceLoader(new DefaultResourceLoader());
            liquibase.afterPropertiesSet();
        }
    }

    private String extensionSchema() {
        return jdbc.queryForObject("select n.nspname from pg_extension e"
            + " join pg_namespace n on n.oid = e.extnamespace where e.extname = 'pg_trgm'", String.class);
    }

    private List<String> indexedSchemas() {
        return jdbc.queryForList("select schemaname from pg_indexes"
            + " where indexname = 'idx_xm_entity_search_text_trgm' order by 1", String.class);
    }

    private static void recreateDatabase(PostgreSQLContainer<?> container) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + DATABASE);
            statement.execute("CREATE DATABASE " + DATABASE);
        }
    }
}
