package com.icthh.xm.ms.entity.web.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

public final class FunctionUtils {

    public static String getFunctionKey(HttpServletRequest request) {
        String path = request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        return new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
    }

}
