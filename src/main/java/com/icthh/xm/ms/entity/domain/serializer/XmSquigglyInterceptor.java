package com.icthh.xm.ms.entity.domain.serializer;

import com.github.bohnman.squiggly.web.SquigglyRequestHolder;
import com.github.bohnman.squiggly.web.SquigglyResponseHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class XmSquigglyInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {

        log.info("Init Squiggly holders in http context");

//        SquigglyRequestHolder.setRequest(request); todo
//        SquigglyResponseHolder.setResponse(response);

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
