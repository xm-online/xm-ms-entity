package com.icthh.xm.ms.entity.service;

import com.github.bohnman.squiggly.web.RequestSquigglyContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class XmSquigglyContextProvider extends RequestSquigglyContextProvider {

    private static final String TO_REPLACE_FILTER = "<REPLACE_ME>";
    private static final String DEFAULT_FILTER = "**";
    private static final String HTTP_FILTER_PARAM_NAME = "fields";

    private final Map<Class, String> defaultFilterByBean;

    public XmSquigglyContextProvider(final Map<Class, String> defaultFilterByBean) {
        super(HTTP_FILTER_PARAM_NAME, TO_REPLACE_FILTER);
        this.defaultFilterByBean = Collections.unmodifiableMap(defaultFilterByBean);
    }

    @Override
    protected String customizeFilter(final String filter, final HttpServletRequest request, final Class beanClass) {

        log.debug("customize json filter for URL: {}, class: {}, current filter: {}",
                  getRequestUri(request), beanClass, filter);

        String updatedFilter = super.customizeFilter(filter, request, beanClass);

        return applyFilter(updatedFilter, request, beanClass);

    }

    @Override
    protected String customizeFilter(final String filter, final Class beanClass) {
        if (TO_REPLACE_FILTER.equals(filter)) {
            String updatedFilter = defaultFilterByBean.getOrDefault(beanClass, DEFAULT_FILTER);
            log.debug("customize json filter for class: {}, updatedFilter: {}", beanClass, updatedFilter);
            return updatedFilter;
        }
        return super.customizeFilter(filter, beanClass);
    }

    @Override
    public boolean isFilteringEnabled() {
        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();
        boolean isOutOfHttpScope = request == null && response == null;
        return isOutOfHttpScope || isFilteringEnabled(request, response);
    }

    @Override
    protected String getFilter(final Class beanClass) {
        if (getRequest() == null) {
            return customizeFilter(TO_REPLACE_FILTER, beanClass);
        }
        return super.getFilter(beanClass);
    }

    // TODO add LEP here
    public String applyFilter(final String filter, final HttpServletRequest request, final Class beanClass) {
        log.debug("apply json filter for URL: {}, class: {}, current filter: {}",
                  getRequestUri(request), beanClass, filter);
        return filter;
    }

    private String getRequestUri(HttpServletRequest request) {
        return Optional.ofNullable(request)
                       .map(HttpServletRequest::getRequestURI)
                       .orElse(null);
    }
}
