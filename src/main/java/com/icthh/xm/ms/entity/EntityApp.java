package com.icthh.xm.ms.entity;

import com.icthh.xm.commons.i18n.spring.config.CommonMessageSourceConfiguration;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.commons.tenant.spring.config.TenantContextConfiguration;
import com.icthh.xm.ms.entity.client.OAuth2InterceptedFeignConfiguration;
import com.icthh.xm.ms.entity.config.ApplicationProperties;
import com.icthh.xm.ms.entity.config.DefaultProfileUtil;
import io.github.jhipster.config.JHipsterConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@ComponentScan(
    value = "com.icthh.xm",
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OAuth2InterceptedFeignConfiguration.class, CommonMessageSourceConfiguration.class})
)
@EnableAutoConfiguration(exclude = {MetricFilterAutoConfiguration.class, MetricRepositoryAutoConfiguration.class})
@EnableConfigurationProperties({LiquibaseProperties.class, ApplicationProperties.class})
@EnableDiscoveryClient
@Import({TenantContextConfiguration.class})
public class EntityApp {

    private static final Logger log = LoggerFactory.getLogger(EntityApp.class);

    private final Environment env;

    private final TenantContextHolder tenantContextHolder;

    public EntityApp(Environment env, TenantContextHolder tenantContextHolder) {
        this.env = env;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Initializes entity.
     *
     * <p>Spring profiles can be configured with a program arguments
     * --spring.profiles.active=your-active-profile
     *
     * <p>You can find more information on how profiles work with JHipster
     * on <a href="http://jhipster.github.io/profiles/">http://jhipster.github.io/profiles/</a>.
     */
    @PostConstruct
    public void initApplication() {
        Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles
            .contains(JHipsterConstants.SPRING_PROFILE_PRODUCTION)) {
            log.error("You have misconfigured your application! It should not run "
                + "with both the 'dev' and 'prod' profiles at the same time.");
        }
        if (activeProfiles.contains(JHipsterConstants.SPRING_PROFILE_DEVELOPMENT) && activeProfiles
            .contains(JHipsterConstants.SPRING_PROFILE_CLOUD)) {
            log.error("You have misconfigured your application! It should not"
                + "run with both the 'dev' and 'cloud' profiles at the same time.");
        }

        initContexts();
    }

    private void initContexts() {
        // init tenant context, by default this is XM super tenant
        TenantContextUtils.setTenant(tenantContextHolder, TenantKey.SUPER);

        // init logger MDC context
        MdcUtils.putRid(MdcUtils.generateRid() + "::" + TenantKey.SUPER.getValue());
    }

    @PreDestroy
    public void destroyApplication() {
        log.info("\n----------------------------------------------------------\n\t"
                + "Application {} is closing"
                + "\n----------------------------------------------------------",
            env.getProperty("spring.application.name"));
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments
     * @throws UnknownHostException if the local host name could not be resolved into an address
     */
    public static void main(String[] args) throws UnknownHostException {

        MdcUtils.putRid();

        SpringApplication app = new SpringApplication(EntityApp.class);
        DefaultProfileUtil.addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        log.info("\n----------------------------------------------------------\n\t"
                + "Application '{}' is running! Access URLs:\n\t"
                + "Local: \t\t{}://localhost:{}\n\t"
                + "External: \t{}://{}:{}\n\t"
                + "Profile(s): \t{}\n----------------------------------------------------------",
            env.getProperty("spring.application.name"),
            protocol,
            env.getProperty("server.port"),
            protocol,
            InetAddress.getLocalHost().getHostAddress(),
            env.getProperty("server.port"),
            env.getActiveProfiles());

        String configServerStatus = env.getProperty("configserver.status");
        log.info("\n----------------------------------------------------------\n\t"
                + "Config Server: \t{}\n----------------------------------------------------------",
            configServerStatus == null ? "Not found or not setup for this application" : configServerStatus);
    }
}
