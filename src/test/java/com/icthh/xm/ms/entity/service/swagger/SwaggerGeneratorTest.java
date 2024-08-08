package com.icthh.xm.ms.entity.service.swagger;

import org.junit.Test;

import java.util.HashMap;

import static com.icthh.xm.ms.entity.web.rest.FunctionResourceIntTest.loadFile;
import static org.junit.Assert.assertEquals;

public class SwaggerGeneratorTest {

    @Test
    public void testTransformJsonSchemaToSwaggerSchema() {
        SwaggerGenerator swaggerGenerator = new SwaggerGenerator();
        String jsonSchema = loadFile("config/testjsonschema.json");
        String result = swaggerGenerator.transformJsonSchemaToSwaggerSchema("TestType", jsonSchema, new HashMap<>());
        System.out.println(result);
    }

}
