package com.icthh.xm.ms.entity.web.filter;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@RequiredArgsConstructor
@Component
@Order(1)
public class ContentCachingWrappingFilter extends OncePerRequestFilter {

    private final ApplicationProperties applicationProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isIgnoredRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer cacheLimit = applicationProperties.getRequestCacheLimit();
        ContentCachingRequestWrapper requestWrapper = cacheLimit != null ?
                                                      new ContentCachingRequestWrapper(request, cacheLimit) :
                                                      new ContentCachingRequestWrapper(request);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private boolean isIgnoredRequest(HttpServletRequest request) {
        String path = request.getServletPath();
        List<String> ignoredPatterns = applicationProperties.getRequestCacheIgnoredPathPatternList();
        if (ignoredPatterns != null && path != null) {
            for (String pattern : ignoredPatterns) {
                if (matcher.match(pattern, path)) {
                    return true;
                }
            }
        }
        return false;
    }
}
