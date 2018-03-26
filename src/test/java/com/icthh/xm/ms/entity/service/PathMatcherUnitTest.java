package com.icthh.xm.ms.entity.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.util.AntPathMatcher;

public class PathMatcherUnitTest {

    @Test
    public void testMatcher() {
        AntPathMatcher matcher = new AntPathMatcher();
        String url = "/config/tenants/XM/entity/specs/xmentityspecs.yml";
        String pattern = "/config/tenants/{tenantName}/entity/specs/xmentityspecs.yml";

        assertTrue(matcher.match(pattern, url));
        assertEquals("XM", matcher.extractUriTemplateVariables(pattern, url).get("tenantName"));
    }

}
