package com.icthh.xm.ms.entity.service.json;

import com.icthh.xm.commons.tenant.YamlMapperUtils;
import com.networknt.schema.SchemaRegistryConfig;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.commons.tenant.JsonMapperUtils;
import com.icthh.xm.ms.entity.service.spec.JsonSchemaGenerationServiceImpl;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.Error;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static com.networknt.schema.SpecificationVersion.DRAFT_4;
import static com.networknt.schema.path.PathType.LEGACY;
import static org.junit.Assert.assertTrue;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaGenerationServiceUnitTest extends AbstractJupiterUnitTest {

    JsonSchemaGenerationService service;

    @BeforeEach
    public void setUp() throws Exception {
        service = new JsonSchemaGenerationServiceImpl();
    }

    @Test
    @SneakyThrows
    public void testXmEntitySpecSchemaGeneration() {
        String jsonSchema = service.generateJsonSchema();
        ObjectMapper objectMapper = YamlMapperUtils.yamlDefaultMapper();
        JsonNode xmentityspec = objectMapper.readTree(loadFile("config/specs/xmentityspec-xm.yml"));

        JsonNode schemaNode = JsonMapperUtils.getDefaultJsonMapper().readTree(jsonSchema);
        SchemaRegistry factory = SchemaRegistry.withDefaultDialect(DRAFT_4,
        builder -> builder.schemaRegistryConfig(SchemaRegistryConfig.builder().pathType(LEGACY).build()));
        Schema schema = factory.getSchema(schemaNode);
        List<Error> report = schema.validate(xmentityspec);

        boolean isSuccess = report.isEmpty();
        assertTrue(report.toString(), isSuccess);
    }

}
