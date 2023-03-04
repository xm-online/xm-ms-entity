package com.icthh.xm.ms.entity.service.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.service.spec.JsonSchemaGenerationServiceImpl;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaGenerationServiceUnitTest extends AbstractUnitTest {

    JsonSchemaGenerationService service;

    @Before
    public void setUp() throws Exception {
        service = new JsonSchemaGenerationServiceImpl();
    }

    @Test
    @SneakyThrows
    public void testXmEntitySpecSchemaGeneration() {
        String jsonSchema = service.generateJsonSchema();
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode xmentityspec = objectMapper.readTree(loadFile("config/specs/xmentityspec-xm.yml"));

        JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonSchema schema = factory.getJsonSchema(schemaNode);
        ProcessingReport report = schema.validate(xmentityspec);

        boolean isSuccess = report.isSuccess();
        assertTrue(report.toString(), isSuccess);
    }

}
