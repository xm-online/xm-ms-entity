package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.service.json.JsonConfigurationListener;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerFunctionGenerator;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@ActiveProfiles("test-entity-spec")
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

    private ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()).configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)

        ;

    @Test
    public void testGenerateSwagger() {
        initConfig(Map.of(
            "/config/tenants/TEST_TENANT/entity/xmentityspec.yml", loadFile("config/swagger/test-entity-spec.yml")
        ));

        tenantContextHolder.getPrivilegedContext().execute(buildTenant("TEST_TENANT"), () -> {
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("https://xm.domain.com:8080");
            var expected = readYml();
            assertEquals(toYml(swagger), toYml(expected));
        });
    }

    @SneakyThrows
    private SwaggerModel readYml() {
        return objectMapper.readValue(loadFile("config/swagger/expected-default.yml"), SwaggerModel.class);
    }

    @SneakyThrows
    private Object toMap(Object input) {
        return objectMapper.convertValue(input, Map.class);
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
        return new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(NON_NULL)
            .setSerializationInclusion(NON_EMPTY)
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(swagger);
    }

    @SneakyThrows
    private static String loadFile(String path) {
        InputStream cfgInputStream = new ClassPathResource(path).getInputStream();
        return IOUtils.toString(cfgInputStream, UTF_8);
    }

}
