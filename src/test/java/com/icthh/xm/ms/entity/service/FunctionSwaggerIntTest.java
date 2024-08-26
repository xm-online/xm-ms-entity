package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.service.json.JsonConfigurationListener;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerFunctionGenerator;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerRefreshableConfiguration.DynamicSwaggerConfiguration;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

@ContextConfiguration(initializers = {FunctionSwaggerIntTest.Initializer.class})
public class FunctionSwaggerIntTest extends AbstractSpringBootTest {

    @Autowired
    private DynamicSwaggerRefreshableConfiguration dynamicSwaggerRefreshableConfiguration;
    @Autowired
    private DynamicSwaggerFunctionGenerator dynamicSwaggerFunctionGenerator;
    @Autowired
    private XmEntitySpecService xmEntitySpecService;
    @Autowired
    private JsonConfigurationListener jsonConfigurationListener;
    @Autowired
    private TenantContextHolder tenantContextHolder;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
            "application.specification-folder-path-pattern=/config/tenants/{tenantName}/entity/xmentityspec/*.yml",
            "application.specification-path-pattern=/config/tenants/{tenantName}/entity/xmentityspec.yml",
            "application.specification-name=xmentityspec.yml"
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Before
    public void before() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST_TENANT");
    }

    @After
    public void tearDown() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testGenerateSwagger() {
        dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
            null);

        initConfig(Map.of(
            "/config/tenants/TEST_TENANT/entity/xmentityspec.yml", loadFile("config/swagger/test-entity-spec.yml")
        ));

        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
        var expected = readExpected("config/swagger/expected-default.yml");
        assertEquals(toYml(expected), toYml(swagger));

    }

    @Test
    public void testOverrideConfiguration() {
        initConfig(Map.of(
            "/config/tenants/TEST_TENANT/entity/xmentityspec.yml", loadFile("config/swagger/test-entity-spec.yml")
        ));

        dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
            loadFile("config/swagger/test-configuration.yml"));
        var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
        assertEquals("4.5.0", swagger.getInfo().getVersion());
        assertEquals("Test swagger", swagger.getInfo().getTitle());
        assertEquals("https://test-env", swagger.getServers().get(0).getUrl());
        assertEquals("https://dev-env", swagger.getServers().get(1).getUrl());
        assertEquals(2, swagger.getServers().size());
        assertEquals(2, swagger.getTags().size());
        assertEquals("External functions", swagger.getTags().get(0).getDescription());
        assertEquals("external", swagger.getTags().get(0).getName());
        assertEquals("Test functions", swagger.getTags().get(1).getDescription());
        assertEquals("test", swagger.getTags().get(1).getName());

    }

    @Test
    public void testExcludeInclude() {
        initConfig(Map.of(
            "/config/tenants/TEST_TENANT/entity/xmentityspec.yml", loadFile("config/swagger/test-entity-spec.yml")
        ));

        {
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                loadFile("config/swagger/test-configuration.yml"));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/folder/v1/TestName", List.of("post", "get"),
                "/entity/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete"),
                "/entity/api/functions/relative/path/{pathVariable}/other/{otherPathVariable}/{notDefinedVariable}/etc",
                List.of("post", "get", "delete", "put")
            ));
        }

        {
            DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
            config.setIncludeTags(List.of("test"));
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                toYml(config));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete"),
                "/entity/api/functions/relative/path/{pathVariable}/other/{otherPathVariable}/{notDefinedVariable}/etc",
                List.of("post", "get", "delete", "put")
            ));
        }

        {
            DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
            config.setExcludeTags(List.of("internal"));
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                toYml(config));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/folder/v1/TestName", List.of("post", "get"),
                "/entity/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete")
            ));
        }

        {
            DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
            config.setIncludeTags(List.of("test"));
            config.setExcludeTags(List.of("internal"));
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                toYml(config));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/check/different/key/with/same/path", List.of("delete", "put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete")
            ));
        }

        {
            DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
            config.setIncludeTags(List.of("test"));
            config.setExcludeTags(List.of("internal", "duplicatePathExclude"));
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                toYml(config));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/check/different/key/with/same/path", List.of("put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete")
            ));
        }

        {
            DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
            config.setIncludeTags(List.of("test", "external"));
            config.setExcludeTags(List.of("internal", "duplicatePathExclude"));
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                toYml(config));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/folder/v1/TestName", List.of("post", "get"),
                "/entity/api/functions/check/different/key/with/same/path", List.of("put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete")
            ));
        }

        {
            DynamicSwaggerConfiguration config = new DynamicSwaggerConfiguration();
            config.setIncludeTags(List.of("test", "external"));
            config.setExcludeTags(List.of("internal", "duplicatePathExclude"));
            config.setExcludeKeyPatterns(List.of("folder/v1/.*"));
            dynamicSwaggerRefreshableConfiguration.onRefresh("/config/tenants/TEST_TENANT/entity/swagger.yml",
                toYml(config));
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            assertPaths(swagger, Map.of(
                "/entity/api/functions/check/different/key/with/same/path", List.of("put"),
                "/entity/api/functions/NameFromKeyOnlyDataInReturn", List.of("post", "delete")
            ));
        }

    }

    private void assertPaths(SwaggerModel swagger, Map<String, List<String>> paths) {
        Set<String> actualPaths = swagger.getPaths().keySet();
        assertEquals(paths.keySet(), actualPaths);
        paths.forEach((path, methods) -> {
            var actualMethods = swagger.getPaths().get(path).keySet();
            assertEquals(new HashSet<>(methods), actualMethods);
        });
    }

    @SneakyThrows
    private SwaggerModel readExpected(String path) {
        return new ObjectMapper(new YAMLFactory()).readValue(loadFile(path), SwaggerModel.class);
    }

    public void initConfig(Map<String, String> files) {
        List.of(jsonConfigurationListener, xmEntitySpecService).forEach(rc -> {
            var keys = files.keySet().stream().filter(rc::isListeningConfiguration).collect(Collectors.toList());
            keys.forEach(k -> rc.onRefresh(k, files.get(k)));
            rc.refreshFinished(keys);
        });
    }

    @SneakyThrows
    private String toYml(Object swagger) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(NON_NULL)
            .setSerializationInclusion(NON_EMPTY)
            .enable(SORT_PROPERTIES_ALPHABETICALLY)
            .enable(ORDER_MAP_ENTRIES_BY_KEYS);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.convertValue(swagger, Map.class));
    }

    @SneakyThrows
    private static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

}
