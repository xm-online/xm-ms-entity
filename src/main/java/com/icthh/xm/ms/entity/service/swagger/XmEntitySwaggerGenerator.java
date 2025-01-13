package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.swagger.JsonSchemaToSwaggerSchemaConverter;
import com.icthh.xm.commons.swagger.impl.AbstractSwaggerGenerator;
import com.icthh.xm.commons.swagger.model.ApiMethod;
import com.icthh.xm.commons.swagger.model.SwaggerContent;
import com.icthh.xm.commons.swagger.model.SwaggerFunction;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import com.icthh.xm.commons.swagger.model.SwaggerResponse;
import com.icthh.xm.ms.entity.domain.FunctionResultContext;
import com.icthh.xm.ms.entity.service.swagger.model.XmEntitySwaggerFunction;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.ms.entity.service.processor.XmEntityDefinitionSpecProcessor.XM_ENTITY_DEFINITION;
import static com.icthh.xm.ms.entity.service.spec.DataSpecJsonSchemaService.DEFINITION_PREFIXES;

@Slf4j
public class XmEntitySwaggerGenerator extends AbstractSwaggerGenerator {

    private final Map<String, Object> DEFAULT_SCHEMA = Map.of("type", "string", "format", "binary");
    private final Set<String> SUCCESS_RESPONSE_CODES = Set.of("200", "201");
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public XmEntitySwaggerGenerator(String baseUrl, DynamicSwaggerConfiguration configuration) {
        super(baseUrl, configuration, new JsonSchemaToSwaggerSchemaConverter(XM_ENTITY_DEFINITION, DEFINITION_PREFIXES));
    }

    @Override
    public void enrichApiMethod(ApiMethod operation, Map<String, SwaggerParameter> pathPrefixParams,
                                SwaggerFunction swaggerFunction, String httpMethod) {

        Optional.ofNullable(swaggerFunction)
            .filter(XmEntitySwaggerFunction.class::isInstance)
            .map(XmEntitySwaggerFunction.class::cast)
            .map(XmEntitySwaggerFunction::getCustomBinaryDataType)
            .ifPresent(type -> updateApiMethodResponse(operation, type));
    }

    private void updateApiMethodResponse(ApiMethod operation, String customBinaryDataType) {
        operation.getResponses().keySet().stream()
            .filter(SUCCESS_RESPONSE_CODES::contains)
            .forEach(code -> operation.getResponses().put(code, getSuccessResponseBody(customBinaryDataType)));

    }

    private SwaggerResponse getSuccessResponseBody(String customBinaryDataType) {
        return new SwaggerResponse(
            Map.of(customBinaryDataType, new SwaggerContent(DEFAULT_SCHEMA)),
            "Successful operation"
        );
    }

    @SneakyThrows
    @Override
    public ObjectNode generateFunctionContext() {
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);
        JsonSchema jsonSchema = schemaGen.generateSchema(FunctionResultContext.class);
        ObjectNode tree = (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(jsonSchema));
        ObjectNode properties = (ObjectNode) tree.get("properties");
        tree.remove("id");
        properties.remove("xmEntity");
        properties.remove("binaryData");
        properties.remove("binaryDataType");
        properties.remove("rid");
        properties.remove("executeTime");
        properties.remove("modelAndView");
        return tree;
    }
}
