package com.icthh.xm.ms.entity.service.swagger;

import com.icthh.xm.commons.config.swagger.DynamicSwaggerConfiguration;
import com.icthh.xm.commons.config.swagger.DynamicSwaggerRefreshableConfiguration;
import com.icthh.xm.commons.swagger.SwaggerGenerator;
import com.icthh.xm.commons.swagger.impl.DefaultDynamicSwaggerFunctionGenerator;
import com.icthh.xm.commons.swagger.model.SwaggerModel;
import com.icthh.xm.commons.swagger.model.SwaggerParameter;
import com.icthh.xm.commons.utils.SwaggerGeneratorUtils;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.icthh.xm.commons.swagger.impl.DynamicSwaggerFunctionGeneratorImpl.DEFAULT_METHODS;
import static com.icthh.xm.commons.utils.CollectionsUtils.nullSafe;
import static com.icthh.xm.commons.utils.Constants.SWAGGER_INFO_TITLE;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.filterHttpMethods;
import static com.icthh.xm.commons.utils.SwaggerGeneratorUtils.getSupportedHttpMethodFilters;
import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Primary
@Service
public class XmEntityDynamicSwaggerFunctionGeneratorImpl extends DefaultDynamicSwaggerFunctionGenerator<FunctionSpec> {

    public static final Set<String> SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS = Set.of(GET.name(), POST.name());
    private static final String XM_ENTITY_SWAGGER_INFO_TITLE = "XM Entity functions api";

    private final XmEntitySpecService xmEntitySpecService;

    public XmEntityDynamicSwaggerFunctionGeneratorImpl(@Value("${spring.application.name}") String appName,
                                                       DynamicSwaggerRefreshableConfiguration dynamicSwaggerService,
                                                       XmEntitySpecService xmEntitySpecService) {
        super(appName, dynamicSwaggerService);
        this.xmEntitySpecService = xmEntitySpecService;
    }

    @Override
    public void enrichSwaggerBody(SwaggerModel swaggerBody) {
        if (SWAGGER_INFO_TITLE.equals(swaggerBody.getInfo().getTitle())) {
            swaggerBody.getInfo().setTitle(XM_ENTITY_SWAGGER_INFO_TITLE);
        }
    }

    @Override
    public SwaggerGenerator getSwaggerGenerator(String baseUrl, String specName) {
        return new XmEntitySwaggerGenerator(baseUrl, getSwaggerConfiguration(specName));
    }

    @Override
    public List<FunctionSpec> getFunctionSpecs(DynamicSwaggerConfiguration swaggerConfig) {
        Collection<TypeSpec> xmEntitySpecifications = xmEntitySpecService.getAllSpecs();
        return xmEntitySpecifications.stream()
            .map(TypeSpec::getFunctions)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(Objects::nonNull)
            .filter(byFilters(swaggerConfig))
            .collect(toList());
    }

    @Override
    public @NotNull String buildPathPrefix(FunctionSpec functionSpec) {
        if (TRUE.equals(functionSpec.getWithEntityId())) {
            return "/entity/api/xm-entities/{idOrKey}/functions";
        }
        return functionSpec.getAnonymous() != null && TRUE.equals(functionSpec.getAnonymous())
            ? "/entity/api/functions/anonymous"
            : "/entity/api/functions";
    }

    @Override
    public @NotNull Map<String, SwaggerParameter> buildPrefixPathParams(FunctionSpec functionSpec) {
        return TRUE.equals(functionSpec.getWithEntityId()) ? buildIdOrKey() : Map.of();
    }

    @Override
    public @NotNull List<String> getFunctionHttpMethods(FunctionSpec functionSpec) {
        List<String> httpMethods = isEmpty(functionSpec.getHttpMethods()) ? DEFAULT_METHODS : functionSpec.getHttpMethods();

        getHttpMethodFilters(functionSpec).stream()
            .filter(f -> f.supported(functionSpec))
            .forEach(f -> f.filter(httpMethods));

        functionSpec.setHttpMethods(httpMethods);
        return httpMethods;
    }

    @Override
    public @NotNull List<String> getFunctionTags(FunctionSpec functionSpec) {
        return nullSafe(functionSpec.getTags());
    }

    @Override
    public String getFunctionName(FunctionSpec functionSpec) {
        Map<String, String> nameMap = nullSafe(functionSpec.getName());
        return Optional.ofNullable(nameMap.get("en"))
            .orElse(nameMap.values().stream().findFirst().orElse(functionSpec.getKey()));
    }

    @Override
    public String getFunctionDescription(FunctionSpec functionSpec) {
        return Optional.ofNullable(functionSpec.getDescription()).orElse(functionSpec.getKey());
    }

    @Override
    public String getFunctionInputJsonSchema(FunctionSpec functionSpec) {
        return functionSpec.getInputSpec();
    }

    @Override
    public String getFunctionOutputJsonSchema(FunctionSpec functionSpec) {
        return functionSpec.getContextDataSpec();
    }

    private List<SwaggerGeneratorUtils.HttpMethodFilter> getHttpMethodFilters(FunctionSpec functionSpec) {
        List<SwaggerGeneratorUtils.HttpMethodFilter> filters = getSupportedHttpMethodFilters();
        filters.add(new SwaggerGeneratorUtils.HttpMethodFilter(
            fs -> TRUE.equals(functionSpec.getWithEntityId()),
            methods -> filterHttpMethods(methods, SUPPORTED_WITH_ENTITY_ID_HTTP_METHODS)
        ));
        return filters;
    }

    private Map<String, SwaggerParameter> buildIdOrKey() {
        return Map.of("idOrKey", new SwaggerParameter(
            "idOrKey", true, Map.of(
            "anyOf", List.of(
                Map.of("type", "integer", "format", "int64"),
                Map.of("type", "string")
            ))));
    }

    /**
     * Predicate to filter function spec by include/exclude key, path and tag
     * @param swaggerConfig dynamic swagger configuration
     * @return predicate
     */
    public static Predicate<? super FunctionSpec> byFilters(DynamicSwaggerConfiguration swaggerConfig) {
        if (swaggerConfig == null) {
            return functionSpec -> true;
        }

        List<String> includeTags = nullSafe(swaggerConfig.getIncludeTags());
        List<String> includePaths = nullSafe(swaggerConfig.getIncludePathPatterns());
        List<String> includeKeys = nullSafe(swaggerConfig.getIncludeKeyPatterns());

        List<String> excludeTags = nullSafe(swaggerConfig.getExcludeTags());
        List<String> excludeKeys = nullSafe(swaggerConfig.getExcludeKeyPatterns());
        List<String> excludePaths = nullSafe(swaggerConfig.getExcludePathPatterns());

        return functionSpec -> {
            boolean matchesInclude = includeTags.isEmpty() && includePaths.isEmpty() && includeKeys.isEmpty()
                || checkFilters(functionSpec, includeTags, includePaths, includeKeys);
            boolean matchesExclude = checkFilters(functionSpec, excludeTags, excludePaths, excludeKeys);

            return matchesInclude && !matchesExclude;
        };
    }

    private static boolean checkFilters(FunctionSpec functionSpec, List<String> tags,
                                        List<String> pathPatterns, List<String> keyPatterns) {

        List<String> funcTags = nullSafe(functionSpec.getTags());
        return tags.stream().anyMatch(funcTags::contains) ||
            pathPatterns.stream().anyMatch(it -> functionSpec.getPath().matches(it)) ||
            keyPatterns.stream().anyMatch(it -> functionSpec.getKey().matches(it));
    }

}
