package com.icthh.xm.ms.entity.config.containers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class PostgresTestContainerConfig {

    private static final DockerImageName IMAGE_NAME = DockerImageName
        .parse("postgres:12.7");

    private static final PostgreSQLContainer container;

    static {
        container = new PostgreSQLContainer(IMAGE_NAME)
            .withDatabaseName("entity")
            .withUsername("sa")
            .withPassword("sa");
        container.start();
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                "spring.datasource.url=" + container.getJdbcUrl(),
                "spring.datasource.username=" + container.getUsername(),
                "spring.datasource.password=" + container.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
            log.info("spring.datasource.url: {}", container.getJdbcUrl());
        }
    }
}
