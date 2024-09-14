package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerRefreshableConfiguration.DynamicSwaggerConfiguration;
import com.icthh.xm.ms.entity.service.swagger.model.ServerObject;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerFunction;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.ApiMethod;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.BodyContent;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.RequestBody;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.SwaggerContent;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.SwaggerResponse;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerParameter;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.POST_URLENCODED;
import static java.lang.Boolean.TRUE;
import static java.lang.String.join;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;
import static org.apache.commons.text.CaseUtils.toCamelCase;

@Slf4j
public class SwaggerGenerator {

    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH", "POST_URLENCODED");
    private static final Set<String> METHODS_WITH_PARAMS = Set.of("GET", "DELETE");

    @Getter
    private final SwaggerModel swaggerBody = new SwaggerModel();
    private final JsonSchemaToSwaggerSchemaConverter jsonSchemaConverter = new JsonSchemaToSwaggerSchemaConverter();
    private final Map<String, Object> definitions = new LinkedHashMap<>();
    private final Map<String, Object> originalDefinitions = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final DynamicSwaggerConfiguration configuration;

    public SwaggerGenerator(String baseUrl, DynamicSwaggerConfiguration configuration) {
        swaggerBody.setServers(List.of(new ServerObject(baseUrl)));
        if (configuration != null) {
            ofNullable(configuration.getInfo()).ifPresent(swaggerBody::setInfo);
            ofNullable(configuration.getTags()).ifPresent(swaggerBody::setTags);
            ofNullable(configuration.getServers()).ifPresent(swaggerBody::setServers);
        }
        swaggerBody.getComponents().setSchemas(definitions);
        this.configuration = configuration;
        addResponses();
    }

    public void generateFunction(String pathPrefix, Map<String, SwaggerParameter> pathPrefixParams, SwaggerFunction swaggerFunction) {
        Map<String, Map<String, ApiMethod>> paths = swaggerBody.getPaths();
        Map<String, ApiMethod> methods = new HashMap<>();
        swaggerFunction.getHttpMethods().forEach(httpMethod -> {
            if (httpMethod.equalsIgnoreCase("POST_URLENCODED") && swaggerFunction.getHttpMethods().contains("POST")) {
                return;
            }

            ApiMethod apiMethod = generateApiMethod(pathPrefixParams, swaggerFunction, httpMethod);
            httpMethod = httpMethod.equalsIgnoreCase("POST_URLENCODED") ? "POST" : httpMethod;
            methods.put(httpMethod.toLowerCase(), apiMethod);
        });

        String path = pathPrefix + swaggerFunction.getPath();
        if (paths.containsKey(path)) {
            paths.get(path).putAll(methods);
        } else {
            paths.put(path, methods);
        }
    }

    private ApiMethod generateApiMethod(Map<String, SwaggerParameter> pathPrefixParams,
                                        SwaggerFunction swaggerFunction,
                                        String httpMethod) {
        ApiMethod operation = new ApiMethod();

        setOperationId(swaggerFunction, httpMethod, operation);
        buildParameters(pathPrefixParams, swaggerFunction, operation, httpMethod);
        operation.setSummary(swaggerFunction.getName());
        operation.setDescription(swaggerFunction.getDescription());
        operation.setTags(swaggerFunction.getTags());
        buildResponse(swaggerFunction, operation, httpMethod);
        if (TRUE.equals(swaggerFunction.getAnonymous())) {
            operation.setSecurity(List.of());
        }

        return operation;
    }

    private void addResponses() {
        addResponse("400", "Bad request. Request invalid by business rules");
        addResponse("401", "Invalid access token");
        addResponse("403", "Forbidden");
        addResponse("404", "Not found");
        addResponse("500", "Internal server error");
        definitions.put("RequestError", Map.of(
            "type", "object",
            "properties", Map.of(
                "error", Map.of(
                    "type", "string"
                ),
                "error_description", Map.of(
                    "type", "string"
                )
            )
        ));
    }

    private void addResponse(String code, String description) {
        Map<String, Object> responses = swaggerBody.getComponents().getResponses();
        responses.put(code, new SwaggerResponse(
            new BodyContent(
                new SwaggerContent(
                    Map.of(
                        "$ref", "#/components/schemas/RequestError"
                    )
                )
            ),
            description
        ));
    }

    private void setOperationId(SwaggerFunction swaggerFunction, String httpMethod, ApiMethod apiMethod) {
        var operationId = swaggerFunction.getOperationId();
        if (swaggerFunction.getHttpMethods().size() > 1) {
            operationId = operationId + httpMethod;
        }
        operationId = join("-", splitByCharacterTypeCamelCase(operationId));
        operationId = toCamelCase(operationId, false, '_', '-', '/', ' ');
        apiMethod.setOperationId(operationId);
    }

    private void buildResponse(SwaggerFunction swaggerFunction, ApiMethod operation, String httpMethod) {
        operation.setResponses(new LinkedHashMap<>());
        generateDefaultResponse(operation);
        if ("POST".equals(httpMethod) || "POST_URLENCODED".equals(httpMethod)) {
            addSuccess(swaggerFunction, operation, "201");
        } else {
            addSuccess(swaggerFunction, operation, "200");
        }
    }

    @SneakyThrows
    private ObjectNode generateFunctionContext() {
        objectMapper.registerModule(new JavaTimeModule());

        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(objectMapper);
        JsonSchema jsonSchema = schemaGen.generateSchema(FunctionContext.class);
        ObjectNode tree = (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(jsonSchema));
        ObjectNode properties = (ObjectNode) tree.get("properties");
        tree.remove("id");
        properties.remove("xmEntity");
        properties.remove("binaryData");
        properties.remove("binaryDataType");
        return tree;
    }

    private void addSuccess(SwaggerFunction swaggerFunction, ApiMethod operation, String code) {
        String successfulMessage = "Successful operation";

        if (swaggerFunction.getCustomBinaryDataType() != null) {
            operation.getResponses().put(code, new SwaggerResponse(Map.of(
                swaggerFunction.getCustomBinaryDataType(), new SwaggerContent(Map.of(
                    "type", "string",
                    "format", "binary"
                ))
            ), successfulMessage));
            return;
        }

        JsonNode jsonNode = jsonSchemaConverter.transformToJsonNode(
            operation.getOperationId(),
            swaggerFunction.getOutputJsonSchema(),
            definitions,
            originalDefinitions
        );
        if (!TRUE.equals(swaggerFunction.getOnlyData())) {
            ObjectNode functionContext = generateFunctionContext();
            ObjectNode properties = (ObjectNode) functionContext.get("properties");
            if (isNotBlank(swaggerFunction.getOutputJsonSchema())) {
                properties.set("data", jsonNode);
            } else {
                ObjectNode objectNode = objectMapper.createObjectNode();
                objectNode.put("type", "object");
                objectNode.put("additionalProperties", true);
                properties.set("data", objectNode);
            }
            jsonNode = functionContext;
        }
        Map<String, Object> schema = convertToMap(jsonNode);
        operation.getResponses().put(code, new SwaggerResponse(new BodyContent(new SwaggerContent(schema)), successfulMessage));
    }

    private void generateDefaultResponse(ApiMethod operation) {
        operation.getResponses().put("400", new SwaggerResponse("#/components/responses/400"));
        operation.getResponses().put("401", new SwaggerResponse("#/components/responses/401"));
        operation.getResponses().put("403", new SwaggerResponse("#/components/responses/403"));
        operation.getResponses().put("404", new SwaggerResponse("#/components/responses/404"));
        operation.getResponses().put("500", new SwaggerResponse("#/components/responses/500"));
    }

    private void buildParameters(Map<String, SwaggerParameter> pathPrefixParams,
                                 SwaggerFunction swaggerFunction,
                                 ApiMethod operation,
                                 String httpMethod) {

        JsonNode jsonNode = jsonSchemaConverter.transformToJsonNode(
            operation.getOperationId(),
            swaggerFunction.getInputJsonSchema(),
            definitions,
            originalDefinitions
        );
        jsonSchemaConverter.inlineRootRef(jsonNode, definitions);
        Map<String, SwaggerParameter> parameters = new LinkedHashMap<>(pathPrefixParams);
        addPathParameters(swaggerFunction, jsonNode, parameters);
        addQueryParameters(jsonNode, parameters, httpMethod);
        if (isNotBlank(swaggerFunction.getInputJsonSchema())) {
            addRequestBody(swaggerFunction, jsonNode, operation, httpMethod);
        }

        operation.setParameters(new ArrayList<>(parameters.values()));
    }

    private void addRequestBody(SwaggerFunction swaggerFunction, JsonNode jsonNode, ApiMethod operation, String httpMethod) {
        if (!METHODS_WITH_BODY.contains(httpMethod)) {
            return;
        }

        Map<String, Object> schema = convertToMap(jsonNode);
        if (httpMethod.equals("POST") && swaggerFunction.getHttpMethods().contains(POST_URLENCODED)) {
            operation.setRequestBody(new RequestBody(true, Map.of(
                "application/x-www-form-urlencoded", new SwaggerContent(schema),
                "application/json", new SwaggerContent(schema)
            )));
        } else if (httpMethod.equals(POST_URLENCODED) && !swaggerFunction.getHttpMethods().contains("POST")) {
            operation.setRequestBody(new RequestBody(true, Map.of(
                "application/x-www-form-urlencoded", new SwaggerContent(schema)
            )));
        } else if (!httpMethod.equals(POST_URLENCODED)) {
            operation.setRequestBody(new RequestBody(true, new BodyContent(new SwaggerContent(schema))));
        }
    }

    private Map<String, Object> convertToMap(JsonNode jsonNode) {
        return objectMapper.convertValue(jsonNode, new TypeReference<>() {});
    }

    private void addQueryParameters(JsonNode jsonNode, Map<String, SwaggerParameter> parameters, String httpMethod) {
        if (METHODS_WITH_PARAMS.contains(httpMethod)) {
            if (jsonNode.isObject() && jsonNode.has("properties")) {
                ObjectNode object = (ObjectNode) jsonNode.get("properties");
                var fields = object.fields();
                while (fields.hasNext()) {
                    var field = fields.next();
                    if (skipUnsupportedFields(field)) {
                        continue;
                    }

                    Map<String, Object> schema = convertToMap(field.getValue());
                    parameters.put(field.getKey(), new SwaggerParameter("query", field.getKey(), true, schema));
                    fields.remove();
                }
            }
        }
    }

    private static boolean skipUnsupportedFields(Map.Entry<String, JsonNode> field) {
        if (field.getKey().equals("$ref")) {
            log.warn("$ref in query parameters is not supported");
            return true;
        }

        if (field.getValue().has("type") && field.getValue().get("type").asText().equals("object")) {
            log.warn("Object in query parameters is not supported");
            return true;
        }

        if (field.getValue().has("type") && field.getValue().get("type").asText().equals("array")) {
            JsonNode items = field.getValue().get("items");
            if (items.has("type") && items.get("type").asText().equals("object")) {
                log.warn("Array of objects in query parameters is not supported");
                return true;
            }

            if (items.has("$ref")) {
                log.warn("Array of $ref in query parameters is not supported");
                return true;
            }
        }

        return false;
    }

    private void addPathParameters(SwaggerFunction swaggerFunction, JsonNode jsonNode, Map<String, SwaggerParameter> parameters) {
        List<String> variablesInPath = getPathVariables(swaggerFunction);
        variablesInPath.forEach(variable -> {
            if (jsonNode.has("properties") && jsonNode.get("properties").has(variable)) {
                ObjectNode object = (ObjectNode) jsonNode.get("properties");
                JsonNode variableSchema = object.remove(variable);
                Map<String, Object> schema = convertToMap(variableSchema);
                parameters.put(variable, new SwaggerParameter(variable, true, schema));
            } else {
                parameters.put(variable, new SwaggerParameter(variable, true, Map.of("type", "string")));
            }
        });
    }

    @NotNull
    private static List<String> getPathVariables(SwaggerFunction swaggerFunction) {
        List<String> variablesInPath = new ArrayList<>();
        StringSubstitutor stringSubstitutor = new StringSubstitutor(
            key -> {
                variablesInPath.add(key);
                return key;
            }, "{", "}", '\\');
        stringSubstitutor.replace(swaggerFunction.getPath());
        return variablesInPath;
    }


}
