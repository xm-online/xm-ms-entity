package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.migration.db.config.DatabaseConfiguration;
import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import com.icthh.xm.ms.entity.repository.entitygraph.EntityGraphRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.config.h2.H2ConfigurationHelper;

import java.sql.SQLException;

@Slf4j
@Configuration
@EnableJpaRepositories(value = "com.icthh.xm.ms.entity.repository",
    repositoryBaseClass = EntityGraphRepositoryImpl.class)
@EntityScan("com.icthh.xm.ms.entity.domain.*")
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class XmDatabaseConfiguration extends DatabaseConfiguration {

    private static final String JPA_PACKAGES = "com.icthh.xm.ms.entity.domain";

    private final Environment env;

    public XmDatabaseConfiguration(Environment env,
                                   JpaProperties jpaProperties,
                                   SchemaResolver schemaResolver) {
        super(env, jpaProperties, schemaResolver);
        this.env = env;
    }

    @Override
    public String getJpaPackages() {
        return JPA_PACKAGES;
    }

    /**
     * Open the TCP port for the H2 database, so it is available remotely.
     *
     * @return the H2 database TCP server.
     * @throws SQLException if the server failed to start.
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @Profile(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT)
    public Object h2TCPServer() throws SQLException {
        String port = getValidPortForH2();
        log.debug("H2 database is available on port {}", port);
        return H2ConfigurationHelper.createServer(port);
    }

    private String getValidPortForH2() {
        int port = Integer.parseInt(env.getProperty("server.port"));
        if (port < 10000) {
            port = 10000 + port;
        } else {
            if (port < 63536) {
                port = port + 2000;
            } else {
                port = port - 2000;
            }
        }
        return String.valueOf(port);
    }

}

