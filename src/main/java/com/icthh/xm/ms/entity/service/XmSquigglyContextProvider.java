package com.icthh.xm.ms.entity.service;

import com.github.bohnman.squiggly.web.RequestSquigglyContextProvider;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class XmSquigglyContextProvider extends RequestSquigglyContextProvider {

    private final Map<Class, String> defaultFilterByBean;
    private final String defaultFilter;

    public XmSquigglyContextProvider(final Map<Class, String> defaultFilterByBean, String defaultFilter) {
        super("fields", defaultFilter);
        this.defaultFilterByBean = Collections.unmodifiableMap(defaultFilterByBean);
        this.defaultFilter = defaultFilter;
    }

    @Override
    protected String customizeFilter(final String filter, final HttpServletRequest request, final Class beanClass) {
//        System.out.println(
//            "customizeFilter $$$$$$$$$$$$$$$ URL: " + request.getContextPath() + " beanClass = " + beanClass
//            + " filter incoming: " + filter);
        return applyFilter(filter, request, beanClass);

    }

    @Override
    public boolean isFilteringEnabled() {
//        boolean filteringEnabled = super.isFilteringEnabled();

        HttpServletRequest request = getRequest();
        HttpServletResponse response = getResponse();

        boolean isOutOfHttpScope = request == null && response == null;
        boolean filteringEnabled = isOutOfHttpScope || isFilteringEnabled(request, response);

//        System.out.println("$$$$$$$$$$$ isFilteringEnabled: " + filteringEnabled);
        return filteringEnabled;
    }

    // TODO add LEP here
    public String applyFilter(final String filter, final HttpServletRequest request, final Class beanClass) {
        System.out.println(
            "applyFilter $$$$$$$$$$$$$$$!!! URL: " + request.getContextPath() + " filter incoming: " + filter);
        return filter;
    }

    @Override
    protected String getFilter(final Class beanClass) {

        // TODO try variant with map
//        String filter = getRequest() == null ? resolveFilter(beanClass) : super.getFilter(beanClass);
        String filter = getRequest() == null ? defaultFilter : super.getFilter(beanClass);

//        System.out.println("resolved filter by class: " + beanClass + " filter = " + filter);

        return filter;
    }

    private String resolveFilter(Class beanClass){
        return defaultFilterByBean.getOrDefault(beanClass, defaultFilter);
    }

}
