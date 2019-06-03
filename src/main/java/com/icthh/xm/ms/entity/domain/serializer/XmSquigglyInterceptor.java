package com.icthh.xm.ms.entity.domain.serializer;

import com.github.bohnman.squiggly.web.SquigglyRequestHolder;
import com.github.bohnman.squiggly.web.SquigglyResponseHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class XmSquigglyInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {

        log.info("Init Squiggly holders in http context");

        SquigglyRequestHolder.setRequest(request);
        SquigglyResponseHolder.setResponse(response);

        return true;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final Object handler, final Exception ex) throws Exception {

        log.info("Clear Squiggly holders in http context");

        SquigglyRequestHolder.removeRequest();
        SquigglyResponseHolder.removeResponse();

    }
}
