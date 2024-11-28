package com.icthh.xm.ms.entity.service.swagger;

import com.icthh.xm.commons.swagger.DynamicSwaggerFunctionGenerator;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.swagger.DynamicSwaggerRefreshableConfiguration.DynamicSwaggerConfiguration;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

@Component
@RequiredArgsConstructor
public class DynamicSwaggerFunctionGeneratorImpl implements DynamicSwaggerFunctionGenerator {

    public static final Set<String> SUPPORTED_HTTP_METHODS = Set.of(
        "GET", "POST", "PUT", "DELETE", "PATCH", "POST_URLENCODED"
    );
    public static final Set<String> SUPPORTED_ANONYMOUS_HTTP_METHODS = Set.of("GET", "POST", "POST_URLENCODED");
    public static final Set<String> SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS = Set.of("GET", "POST");

    private final DynamicSwaggerRefreshableConfiguration dynamicSwaggerConfiguration;
    private final XmEntitySpecService xmEntitySpecService;

    public SwaggerModel generateSwagger(String baseUrl) {
        return generateSwagger(baseUrl, xmEntitySpecService.getAllSpecs());
    }

    public SwaggerModel generateSwagger(String baseUrl, Collection<TypeSpec> specs) {
        DynamicSwaggerConfiguration configuration = dynamicSwaggerConfiguration.getConfiguration();
        SwaggerGenerator swaggerGenerator = new SwaggerGenerator(baseUrl, configuration);
        List<FunctionSpec> functions = specs.stream()
            .map(TypeSpec::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .filter(byFilters(configuration))
            .collect(toList());

        functions.forEach(it -> generateForFunction(it, swaggerGenerator));
        return swaggerGenerator.getSwaggerBody();
    }

     public void generateForFunction(FunctionSpec functionSpec, SwaggerGenerator swaggerGenerator) {
        List<String> httpMethods = isEmpty(functionSpec.getHttpMethods()) ? List.of("GET", "POST") : functionSpec.getHttpMethods();
        functionSpec.setHttpMethods(httpMethods);

        String path = isNotBlank(functionSpec.getPath()) ? functionSpec.getPath() : functionSpec.getKey();
        path = makeAsPath(path);

        Map<String, SwaggerParameter> pathPrefixParams = buildPrefixPathParams(functionSpec);
        String prefix = buildPathPrefix(functionSpec);
        filterHttpMethods(functionSpec);

        List<String> tags = functionSpec.getTags();
        tags = tags != null ? tags : new ArrayList<>();

        String name = getName(functionSpec);

        SwaggerFunction swaggerFunction = new SwaggerFunction(
            functionSpec.getKey(),
            path,
            name,
            functionSpec.getKey(),
            functionSpec.getInputSpec(),
            functionSpec.getContextDataSpec(),
            tags,
            functionSpec.getHttpMethods(),
            functionSpec.getBinaryDataType(),
            functionSpec.getOnlyData(),
            functionSpec.getAnonymous()
        );
        swaggerGenerator.generateFunction(prefix, pathPrefixParams, swaggerFunction);
    }

    @NotNull
    private Map<String, SwaggerParameter> buildPrefixPathParams(FunctionSpec functionSpec) {
        Map<String, SwaggerParameter> pathPrefixParams = Map.of();
        if (TRUE.equals(functionSpec.getWithEntityId())) {
            pathPrefixParams = buildIdOrKey();
        }
        return pathPrefixParams;
    }

    @NotNull
    private static String buildPathPrefix(FunctionSpec functionSpec) {
        String prefix = "/entity/api/functions";
        if (TRUE.equals(functionSpec.getWithEntityId())) {
            prefix = "/entity/api/xm-entities/{idOrKey}/functions";
        } else if (TRUE.equals(functionSpec.getAnonymous())) {
            prefix = prefix + "/anonymous";
        }
        return prefix;
    }

    private static void filterHttpMethods(FunctionSpec functionSpec) {
        if (TRUE.equals(functionSpec.getWithEntityId())) {
            filterHttpMethods(functionSpec, SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS);
        } else if (TRUE.equals(functionSpec.getAnonymous())) {
            filterHttpMethods(functionSpec, SUPPORTED_ANONYMOUS_HTTP_METHODS);
        }
        filterHttpMethods(functionSpec, SUPPORTED_HTTP_METHODS);
    }

    private static String getName(FunctionSpec functionSpec) {
        Map<String, String> nameMap = functionSpec.getName();
        nameMap = nameMap != null ? nameMap : Map.of();
        String name = nameMap.get("en");
        if (name == null) {
            name = nameMap.values().stream().findFirst().orElse(functionSpec.getKey());
        }
        return name;
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

    private Predicate<? super FunctionSpec> byFilters(DynamicSwaggerConfiguration configuration) {
        return functionSpec -> {
            if (configuration == null) {
                return true;
            }

            List<String> includeTags = nullSafe(configuration.getIncludeTags());
            List<String> includePaths = nullSafe(configuration.getIncludePathPatterns());
            List<String> includeKeys = nullSafe(configuration.getIncludeKeyPatterns());

            List<String> excludeTags = nullSafe(configuration.getExcludeTags());
            List<String> excludeKeys = nullSafe(configuration.getExcludeKeyPatterns());
            List<String> excludePaths = nullSafe(configuration.getExcludePathPatterns());

            if (includeTags.isEmpty() && includePaths.isEmpty() && includeKeys.isEmpty()) {
                return !checkFilters(functionSpec, excludeTags, excludePaths, excludeKeys);
            } else {
                return checkFilters(functionSpec, includeTags, includePaths, includeKeys)
                    && !checkFilters(functionSpec, excludeTags, excludePaths, excludeKeys);
            }
        };
    }

    private boolean checkFilters(FunctionSpec functionSpec, List<String> tags, List<String> pathPatterns, List<String> keyPatterns) {
        List<String> funcTags = nullSafe(functionSpec.getTags());
        if (tags.stream().anyMatch(funcTags::contains)) {
            return true;
        }
        if (pathPatterns.stream().anyMatch(it -> functionSpec.getPath().matches(it))) {
            return true;
        }
        return keyPatterns.stream().anyMatch(it -> functionSpec.getKey().matches(it));
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }

}
