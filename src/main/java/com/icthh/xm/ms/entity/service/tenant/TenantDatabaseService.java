package com.icthh.xm.ms.entity.service.tenant;

import static com.icthh.xm.ms.entity.config.Constants.CHANGE_LOG_PATH;
import static org.apache.commons.lang3.time.StopWatch.createStarted;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.config.tenant.SchemaDropResolver;
import com.icthh.xm.ms.entity.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
@IgnoreLogginAspect
public class TenantDatabaseService {

    private DataSource dataSource;
    private LiquibaseProperties liquibaseProperties;
    private ResourceLoader resourceLoader;
    private SchemaDropResolver schemaDropResolver;

    /**
     * Create database schema for tenant.
     *
     * @param tenant - the tenant
     */
    public void create(Tenant tenant) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:CreateTenant:schema tenantKey={}", tenant.getTenantKey());
        DatabaseUtil.createSchema(dataSource, tenant.getTenantKey());
        log.info("STOP - SETUP:CreateTenant:schema tenantKey={}, time={}ms", tenant.getTenantKey(),
            stopWatch.getTime());
        try {
            stopWatch.reset();
            stopWatch.start();
            log.info("START - SETUP:CreateTenant:liquibase tenantKey={}", tenant.getTenantKey());
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setResourceLoader(resourceLoader);
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog(CHANGE_LOG_PATH);
            liquibase.setContexts(liquibaseProperties.getContexts());
            liquibase.setDefaultSchema(tenant.getTenantKey());
            liquibase.setDropFirst(liquibaseProperties.isDropFirst());
            liquibase.setShouldRun(true);
            liquibase.afterPropertiesSet();
            log.info("STOP - SETUP:CreateTenant:liquibase tenantKey={}, time={}ms", tenant.getTenantKey(),
                stopWatch.getTime());
        } catch (LiquibaseException e) {
            throw new RuntimeException("Can not migrate database for creation tenant " + tenant.getTenantKey(), e);
        }
    }


    /**
     * Drop database schema for tenant.
     *
     * @param tenantKey - the tenant key
     */
    public void drop(String tenantKey) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:DeleteTenant:liquibase tenantKey={}", tenantKey);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(schemaDropResolver.getSchemaDropCommand(), tenantKey));
        } catch (SQLException e) {
            throw new RuntimeException("Can not connect to database", e);
        }
        log.info("STOP - SETUP:DeleteTenant:liquibase tenantKey={}, time={}ms", tenantKey, stopWatch.getTime());
    }

}
