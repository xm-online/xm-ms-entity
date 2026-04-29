package com.icthh.xm.ms.entity.config;

import com.icthh.xm.commons.migration.db.config.DatabaseConfiguration;
import com.icthh.xm.commons.migration.db.tenant.SchemaResolver;
import com.icthh.xm.ms.entity.repository.entitygraph.EntityGraphRepositoryImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@EnableJpaRepositories(value = "com.icthh.xm.ms.entity.repository",
    repositoryBaseClass = EntityGraphRepositoryImpl.class)
@EntityScan(basePackages = "com.icthh.xm.ms.entity.domain")
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class XmDatabaseConfiguration extends DatabaseConfiguration {

    private static final String JPA_PACKAGES = "com.icthh.xm.ms.entity.domain";

    public XmDatabaseConfiguration(Environment env,
                                   JpaProperties jpaProperties,
                                   SchemaResolver schemaResolver) {
        super(env, jpaProperties, schemaResolver);
    }

    @Override
    public String getJpaPackages() {
        return JPA_PACKAGES;
    }

}