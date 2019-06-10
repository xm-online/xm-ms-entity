package com.icthh.xm.ms.entity.util;

import static com.icthh.xm.commons.tenant.TenantContextUtils.assertTenantKeyValid;
import static com.icthh.xm.ms.entity.config.Constants.DDL_CREATE_SCHEMA;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import lombok.experimental.UtilityClass;

/**
 * Utility for database operations.
 */
@UtilityClass
@SuppressWarnings("squid:S1118") // private constructor generated by lombok
public final class DatabaseUtil {

    /**
     * Creates new database scheme.
     *
     * @param dataSource the datasource
     * @param name       schema name
     */
    public static void createSchema(DataSource dataSource, String name) {
        assertTenantKeyValid(name);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(DDL_CREATE_SCHEMA, name));
        } catch (SQLException e) {
            throw new RuntimeException("Can not connect to database", e);
        }
    }
}
