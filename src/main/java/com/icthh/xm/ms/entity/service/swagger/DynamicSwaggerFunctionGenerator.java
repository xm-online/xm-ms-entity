package com.icthh.xm.ms.entity.service.swagger;

import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerFunction;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerModel;
import com.icthh.xm.ms.entity.service.swagger.model.SwaggerParameter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

@Component
@RequiredArgsConstructor
public class DynamicSwaggerFunctionGenerator {

    public static final Set<String> SUPPORTED_HTTP_METHODS = Set.of(
        "GET", "POST", "PUT", "DELETE", "PATCH", "POST_URLENCODED"
    );
    public static final Set<String> SUPPORTED_ANONYMOUS_HTTP_METHODS = Set.of("GET", "POST", "POST_URLENCODED");
    public static final Set<String> SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS = Set.of("GET", "POST");

    private final DynamicSwaggerRefreshableConfiguration dynamicSwaggerConfiguration;

    public SwaggerModel generateSwagger(String baseUrl, Collection<TypeSpec> specs) {
        // implement function response
        // implement binary data type
        SwaggerGenerator swaggerGenerator = new SwaggerGenerator(baseUrl, dynamicSwaggerConfiguration.getConfiguration());
        for (var spec : specs) {
            List<FunctionSpec> functions = spec.getFunctions();
            functions = functions != null ? functions : new ArrayList<>();
            functions.forEach(it -> generateForFunction(it, swaggerGenerator));
        }
        return swaggerGenerator.getSwaggerBody();
    }

    public void generateForFunction(FunctionSpec functionSpec, SwaggerGenerator swaggerGenerator) {
        List<String> httpMethods = isEmpty(functionSpec.getHttpMethods()) ? List.of("GET", "POST") : functionSpec.getHttpMethods();
        functionSpec.setHttpMethods(httpMethods);

        String path = isNotBlank(functionSpec.getPath()) ? functionSpec.getPath() : functionSpec.getKey();
        path = makeAsPath(path);

        String prefix = "/entity/api/functions";
        Map<String, SwaggerParameter> pathPrefixParams = Map.of();
        if (TRUE.equals(functionSpec.getWithEntityId())) {
            prefix = "/entity/api/xm-entities/{idOrKey}/functions";
            pathPrefixParams = buildIdOrKey();
            filterHttpMethods(functionSpec, SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS);
        } else if (TRUE.equals(functionSpec.getAnonymous())) {
            prefix = prefix + "/anonymous";
            filterHttpMethods(functionSpec, SUPPORTED_ANONYMOUS_HTTP_METHODS);
        }
        filterHttpMethods(functionSpec, SUPPORTED_HTTP_METHODS);

        List<String> tags = functionSpec.getTags();
        tags = tags != null ? tags : new ArrayList<>();

        Map<String, String> nameMap = functionSpec.getName();
        nameMap = nameMap != null ? nameMap : Map.of();
        String name = nameMap.get("en");
        if (name == null) {
            name = nameMap.values().stream().findFirst().orElse(functionSpec.getKey());
        }

        SwaggerFunction swaggerFunction = new SwaggerFunction(
            functionSpec.getKey(),
            path,
            name,
            functionSpec.getInputSpec(),
            functionSpec.getContextDataSpec(),
            tags,
            functionSpec.getHttpMethods(),
            functionSpec.getBinaryDataType()
        );
        swaggerGenerator.generateFunction(prefix, pathPrefixParams, swaggerFunction);
    }

    private Map<String, SwaggerParameter> buildIdOrKey() {
        return Map.of("idOrKey", new SwaggerParameter(
            "idOrKey", true, Map.of(
            "anyOf", List.of(
                Map.of("type", "integer", "format", "int64"),
                Map.of("type", "string")
            ))));
    }

    private static void filterHttpMethods(FunctionSpec functionSpec, Set<String> supportedMethods) {
        List<String> httpMethods = functionSpec.getHttpMethods().stream()
            .filter(supportedMethods::contains).collect(toList());
        functionSpec.setHttpMethods(httpMethods);
    }

    private static String makeAsPath(String path) {
        path = stripStart(path, "/");
        path = stripEnd(path, "/");
        path = "/" + path;
        return path;
    }

}
