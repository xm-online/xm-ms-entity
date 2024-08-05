package com.icthh.xm.ms.entity.service.swagger;

import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.swagger.SwaggerGenerator.SwaggerFunction;

import java.util.Map;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

public class SwaggerFunctionGenerator {

    public void generateSwagger(String baseUrl, XmEntitySpec xmEntitySpec) {

    }

    public void generateForFunction(FunctionSpec functionSpec) {
        String path = isNotBlank(functionSpec.getPath()) ? functionSpec.getPath() : functionSpec.getKey();
        path = makeAsPath(path);
        if (TRUE.equals(functionSpec.getAnonymous())) {
            path = "/anonymous" + path;
        }

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
            functionSpec.getBinaryDataType()
        );
    }

    private static String makeAsPath(String path) {
        path = stripStart(path, "/");
        path = stripEnd(path, "/");
        path = "/" + path;
        return path;
    }

}
