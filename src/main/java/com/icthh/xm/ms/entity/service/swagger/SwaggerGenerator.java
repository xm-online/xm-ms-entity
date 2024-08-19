package com.icthh.xm.ms.entity.service.swagger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerRefreshableConfiguration.DynamicSwaggerConfiguration;
import com.icthh.xm.ms.entity.service.swagger.model.ServerObject;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerFunction;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.ApiMethod;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.RequestBody;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.RequestBodyContent;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.SwaggerContent;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel.SwaggerResponse;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerParameter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.text.CaseUtils.toCamelCase;

@Slf4j
public class SwaggerGenerator {

    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");
    private static final Set<String> METHODS_WITH_PARAMS = Set.of("GET", "POST_URLENCODED", "DELETE");

    @Getter
    private final SwaggerModel swaggerBody = new SwaggerModel();
    private final JsonSchemaToSwaggerSchemaConverter jsonSchemaConverter = new JsonSchemaToSwaggerSchemaConverter();
    private final Map<String, Object> definitions = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SwaggerGenerator(String baseUrl, DynamicSwaggerConfiguration configuration) {
        swaggerBody.setServers(List.of(new ServerObject(baseUrl)));
        if (configuration != null) {
            ofNullable(configuration.getInfo()).ifPresent(swaggerBody::setInfo);
            ofNullable(configuration.getTags()).ifPresent(swaggerBody::setTags);
            ofNullable(configuration.getServers()).ifPresent(swaggerBody::setServers);
        }
        swaggerBody.getComponents().setSchemas(definitions);
        addResponses();
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
            Map.of(
                "application/json", new SwaggerContent(Map.of(
                    "$ref", "#/components/schemas/RequestError"
                ))
            ),
            description
        ));
    }

    public void generateFunction(String pathPrefix, Map<String, SwaggerParameter> pathPrefixParams, SwaggerFunction swaggerFunction) {
        Map<String, Map<String, ApiMethod>> paths = swaggerBody.getPaths();
        Map<String, ApiMethod> methods = new HashMap<>();
        swaggerFunction.getHttpMethods().forEach(httpMethod -> {
            ApiMethod apiMethod = generateApiMethod(pathPrefixParams, swaggerFunction, httpMethod);
            if (httpMethod.equalsIgnoreCase("POST_URLENCODED")) {
                httpMethod = "POST";
            }
            methods.put(httpMethod.toLowerCase(), apiMethod);
        });

        paths.put(pathPrefix + swaggerFunction.getPath(), methods);
    }

    private void setOperationId(SwaggerFunction swaggerFunction, String httpMethod, ApiMethod apiMethod) {
        var operationId = swaggerFunction.getOperationId();
        if (swaggerFunction.getHttpMethods().size() > 1) {
            operationId = operationId + "-" + httpMethod;
        }
        operationId = toCamelCase(operationId, false, '_', '-', '/', ' ');
        apiMethod.setOperationId(operationId);
    }

    private ApiMethod generateApiMethod(Map<String, SwaggerParameter> pathPrefixParams,
                                        SwaggerFunction swaggerFunction,
                                        String httpMethod) {
        ApiMethod operation = new ApiMethod();

        setOperationId(swaggerFunction, httpMethod, operation);
        buildParameters(pathPrefixParams, swaggerFunction, operation, httpMethod);
        operation.setSummary(swaggerFunction.getName());
        operation.setTags(swaggerFunction.getTags());
        buildResponse(operation);

        return operation;
    }

    private void buildResponse(ApiMethod operation) {
        operation.setResponses(new LinkedHashMap<>());
        generateDefaultResponse(operation);
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
        if (isBlank(swaggerFunction.getInputJsonSchema())) {
            operation.setParameters(new ArrayList<>(pathPrefixParams.values()));
            return;
        }

        JsonNode jsonNode = jsonSchemaConverter.transformToJsonNode(
            operation.getOperationId(),
            swaggerFunction.getInputJsonSchema(),
            definitions
        );
        Map<String, SwaggerParameter> parameters = new LinkedHashMap<>(pathPrefixParams);
        addPathParameters(swaggerFunction, jsonNode, parameters);
        addQueryParameters(jsonNode, parameters, httpMethod);
        addRequestBody(jsonNode, operation, httpMethod);

        operation.setParameters(new ArrayList<>(parameters.values()));
    }

    private void addRequestBody(JsonNode jsonNode, ApiMethod operation, String httpMethod) {
        if (METHODS_WITH_BODY.contains(httpMethod)) {
            Map<String, Object> schema = convertToMap(jsonNode);
            operation.setRequestBody(new RequestBody(true, new RequestBodyContent(new SwaggerContent(schema))));
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
            if (jsonNode.get("properties").has(variable)) {
                ObjectNode object = (ObjectNode) jsonNode.get("properties");
                JsonNode variableSchema = object.remove(variable);
                Map<String, Object> schema = convertToMap(variableSchema);
                parameters.put(variable, new SwaggerParameter(variable, true, schema));
            }
        });
    }

    @NotNull
    private static List<String> getPathVariables(SwaggerFunction swaggerFunction) {
        List<String> variablesInPath = new ArrayList<>();
        StringSubstitutor stringSubstitutor = new StringSubstitutor(
            key -> {
                variablesInPath.add(key);
                return "{" + key + "}";
            }, "{", "}", '\\');
        stringSubstitutor.replace(swaggerFunction.getPath());
        return variablesInPath;
    }


}
