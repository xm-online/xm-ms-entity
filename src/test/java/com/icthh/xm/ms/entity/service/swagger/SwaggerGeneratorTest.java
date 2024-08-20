package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.json.JsonConfigurationListener;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecContextService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.icthh.xm.commons.tenant.TenantContextUtils.buildTenant;
import static com.icthh.xm.ms.entity.web.rest.FunctionResourceIntTest.loadFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;

@ActiveProfiles("test-entity-spec")
public class SwaggerGeneratorTest extends AbstractSpringBootTest {

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Autowired
    private JsonConfigurationListener jsonConfigurationListener;

    @Autowired
    private XmEntitySpecContextService xmEntitySpecContextService;

    @Autowired
    private DynamicSwaggerFunctionGenerator dynamicSwaggerFunctionGenerator;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Test
    public void testTransformJsonSchemaToSwaggerSchema() {
        JsonSchemaToSwaggerSchemaConverter swaggerGenerator = new JsonSchemaToSwaggerSchemaConverter();
        String jsonSchema = loadFile("config/testjsonschema.json");
        String result = swaggerGenerator.transformToSwaggerJson("TestType", jsonSchema, new HashMap<>());
        System.out.println(result);
    }

    @Test
    @SneakyThrows
    public void testSwaggerGenerator() {
        var files = loadByPath("/home/ssenko/work/XM2/ispgest-xm-configuration-dev/config/tenants/ROOT0CORE");
        //var files = loadByPath("/home/ssenko/work/OKKOON/config-repo/config/tenants/BILLING");
        List.of(jsonConfigurationListener, xmEntitySpecService).forEach(rc -> {
            var keys = files.keySet().stream().filter(rc::isListeningConfiguration).collect(Collectors.toList());
            keys.forEach(k -> rc.onRefresh(k, files.get(k)));
            rc.refreshFinished(keys);
        });

        tenantContextHolder.getPrivilegedContext().execute(buildTenant("TEST_TENANT"), () -> {
            var map = xmEntitySpecContextService.typesByTenant("TEST_TENANT");
            var swagger = dynamicSwaggerFunctionGenerator.generateSwagger("http://example.com", map.values());
            var yml = toYml(swagger);
            System.out.println(yml);
            var file = new File("/home/ssenko/work/xm-ms-entity/swagger.yml");
            try {
                writeStringToFile(file, yml, UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
    private Map<String, String> loadByPath(String baseDirPath) {
        Map<String, String> fileMap = new HashMap<>();

        String newBasePath = "/config/tenants/TEST_TENANT/";
        baseDirPath = StringUtils.stripEnd(baseDirPath, "/");

        File baseDir = new File(baseDirPath + "/entity/xmentityspec");
        if (baseDir.exists() && baseDir.isDirectory()) {
            Collection<File> files = FileUtils.listFiles(baseDir, null, true);
            for (File file : files) {
                String content = FileUtils.readFileToString(file, "UTF-8");
                String relativePath = baseDir.toURI().relativize(file.toURI()).getPath();
                String newPath = FilenameUtils.concat(newBasePath + "/entity/xmentityspec", relativePath);
                fileMap.put(newPath, content);
            }
        }

        var mainFile = new File(baseDirPath + "/entity/xmentityspec.yml");
        if (mainFile.exists()) {
            String content = FileUtils.readFileToString(mainFile, "UTF-8");
            fileMap.put("/config/tenants/TEST_TENANT/entity/xmentityspec.yml", content);
        }

        return fileMap;
    }

}
