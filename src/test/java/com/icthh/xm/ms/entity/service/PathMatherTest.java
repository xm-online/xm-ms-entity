package com.icthh.xm.ms.entity.service;

import org.junit.Test;
import org.springframework.util.AntPathMatcher;

public class PathMatherTest {

    @Test
    public void test() {
        AntPathMatcher matcher = new AntPathMatcher();
        String url = "/config/tenants/XM/entity/specs/xmentityspecs.yml";
        String pattern = "/config/tenants/{tenantName}/entity/specs/xmentityspecs.yml";
        System.out.println(matcher.match(pattern, url));
        System.out.println(matcher.extractUriTemplateVariables(pattern, url).get("tenantName"));
    }

}
