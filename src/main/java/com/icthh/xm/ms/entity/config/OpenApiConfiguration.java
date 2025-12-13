package com.icthh.xm.ms.entity.config;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.apidoc.customizer.JHipsterOpenApiCustomizer;

import java.util.List;

@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_API_DOCS)
public class OpenApiConfiguration {

    public static final String API_FIRST_PACKAGE = "com.icthh.xm.ms.entity.web.api";

    @Bean
    @ConditionalOnMissingBean(name = "apiFirstGroupedOpenAPI")
    public GroupedOpenApi apiFirstGroupedOpenAPI(
        JHipsterOpenApiCustomizer jhipsterOpenApiCustomizer,
        JHipsterProperties jHipsterProperties
    ) {
        JHipsterProperties.ApiDocs properties = jHipsterProperties.getApiDocs();
        return GroupedOpenApi
            .builder()
            .group("openapi")
            .addOpenApiCustomizer(jhipsterOpenApiCustomizer)
            .packagesToScan(API_FIRST_PACKAGE)
            .pathsToMatch(properties.getDefaultIncludePattern())
            .build();
    }

    /**
     * Add service name to swagger server url
     * @param appName   serviceName
     * @return          OpenApiCustomizer
     */
    @Bean
    public OpenApiCustomizer customServerCustomizer(@Value("${spring.application.name:}") String appName) {
        return openApi -> {
            if (StringUtils.isNotBlank(appName)) {
                List<Server> customizedServiceBasePath = openApi.getServers().stream()
                    .map(Server::getUrl)
                    .map(url -> buildOpenApiServer(url, appName))
                    .toList();
                openApi.setServers(customizedServiceBasePath);
            }
        };
    }

    private Server buildOpenApiServer(String url, String appName) {
        return new Server()
            .url(url.replaceAll("/+$", "") + "/" + appName)
            .description("Customized service base path");
    }
}
