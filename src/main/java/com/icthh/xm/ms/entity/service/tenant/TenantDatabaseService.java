package com.icthh.xm.ms.entity.service.tenant;

import static com.icthh.xm.commons.tenant.TenantContextUtils.assertTenantKeyValid;
import static com.icthh.xm.ms.entity.config.Constants.CHANGE_LOG_PATH;
import static org.apache.commons.lang3.time.StopWatch.createStarted;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.migration.db.tenant.DropSchemaResolver;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.EntityState;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.util.DatabaseUtil;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import javax.sql.DataSource;

@Service
@AllArgsConstructor
@Slf4j
@IgnoreLogginAspect
public class TenantDatabaseService {

    private DataSource dataSource;
    private LiquibaseProperties liquibaseProperties;
    private ResourceLoader resourceLoader;
    private DropSchemaResolver schemaDropResolver;
    private TenantContextHolder tenantContextHolder;
    private ProfileService profileService;

    /**
     * Create database schema for tenant.
     *
     * @param tenant the tenant
     */
    public void createSchema(Tenant tenant) {
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
     * @param tenantKey the tenant key
     */
    public void dropSchema(String tenantKey) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:DeleteTenant:liquibase tenantKey={}", tenantKey);
        assertTenantKeyValid(tenantKey);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(schemaDropResolver.getSchemaDropCommand(), tenantKey));
        } catch (SQLException e) {
            throw new RuntimeException("Can not connect to database", e);
        }
        log.info("STOP - SETUP:DeleteTenant:liquibase tenantKey={}, time={}ms", tenantKey, stopWatch.getTime());
    }

    /**
     * Create profile for default user.
     * @param tenantKey the tenant key
     */
    public void createProfile(String tenantKey) {
        StopWatch stopWatch = createStarted();
        log.info("START - SETUP:CreateTenant:default profile tenantKey={}", tenantKey);
        String oldTenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        try {
            TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
            profileService.save(buildProfileForDefaultUser(tenantKey));
            log.info("STOP - SETUP:CreateTenant:default profile tenantKey={}, time={}ms",
                tenantKey, stopWatch.getTime());
        } catch (Exception e) {
            log.error("STOP  - SETUP:CreateTenant:default profile tenantKey: {}, result: FAIL, error: {}, time = {} ms",
                tenantKey, e.getMessage(), stopWatch.getTime());
            throw e;
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
            TenantContextUtils.setTenant(tenantContextHolder, oldTenantKey);
        }
    }

    private Profile buildProfileForDefaultUser(String tenantKey) {
        XmEntity entity = new XmEntity();
        entity.setTypeKey("ACCOUNT.USER");
        entity.setKey("ACCOUNT.USER-1");
        entity.setName("Administrator");
        entity.setStateKey(EntityState.NEW.name());
        entity.setStartDate(Instant.now());
        entity.setUpdateDate(Instant.now());
        entity.setCreatedBy(Constants.SYSTEM_ACCOUNT);

        Profile profile = new Profile();
        profile.setXmentity(entity);
        profile.setUserKey(tenantKey.toLowerCase());
        return profile;
    }
}
