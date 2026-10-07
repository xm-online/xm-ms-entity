package com.icthh.xm.ms.entity.web.filter;

import com.icthh.xm.ms.entity.config.ApplicationProperties;
import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
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
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            logLargeResponse(request, responseWrapper, stopWatch.getTime());
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Logs an easily-greppable WARN for any response body at or above
     * {@link ApplicationProperties#getLargeResponseLogThresholdBytes()}. Intended to let a huge
     * payload (e.g. an unbounded/overly broad search or export) be identified directly from this
     * instance's logs, rather than only showing up as a resource-contention symptom (heap/circuit
     * breaker spikes) on the shared Elasticsearch cluster.
     */
    private void logLargeResponse(HttpServletRequest request,
                                  ContentCachingResponseWrapper responseWrapper,
                                  long durationMs) {
        int responseSize = responseWrapper.getContentSize();
        long threshold = applicationProperties.getLargeResponseLogThresholdBytes();
        if (responseSize >= threshold) {
            log.warn("Large HTTP response: method: {}, uri: {}, query: {}, status: {}, "
                    + "responseSizeBytes: {}, duration: {} ms",
                request.getMethod(), request.getRequestURI(), request.getQueryString(),
                responseWrapper.getStatus(), responseSize, durationMs);
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
