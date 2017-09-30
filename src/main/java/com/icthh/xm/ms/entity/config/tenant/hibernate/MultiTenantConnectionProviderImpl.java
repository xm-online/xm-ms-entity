package com.icthh.xm.ms.entity.config.tenant.hibernate;

import com.icthh.xm.ms.entity.config.tenant.SchemaChangeResolver;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {

    private static final long serialVersionUID = 1L;

    @Autowired
    private transient DataSource dataSource;

    @Autowired
    private transient SchemaChangeResolver resolver;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {

        final Connection connection = getAnyConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format(resolver.getSchemaSwitchCommand(), tenantIdentifier));
        } catch (SQLException e) {
            throw new HibernateException(
                "Could not alter JDBC connection to specified schema [" + tenantIdentifier + "]", e
            );
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(String.format(resolver.getSchemaSwitchCommand(), TenantContext.DEFAULT_TENANT));
        } catch (SQLException e) {
            throw new HibernateException(
                "Could not alter JDBC connection to specified schema [" + tenantIdentifier + "]", e);
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return true;
    }

}
