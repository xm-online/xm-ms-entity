package com.icthh.xm.ms.entity.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.service.spec.JsonSchemaGenerationServiceImpl;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
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
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode xmentityspec = objectMapper.readTree(loadFile("config/specs/xmentityspec-xm.yml"));

        JsonNode schemaNode = new ObjectMapper().readTree(jsonSchema);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        JsonSchema schema = factory.getSchema(schemaNode);
        Set<ValidationMessage> report = schema.validate(xmentityspec);

        boolean isSuccess = report.isEmpty();
        assertTrue(report.toString(), isSuccess);
    }

}
