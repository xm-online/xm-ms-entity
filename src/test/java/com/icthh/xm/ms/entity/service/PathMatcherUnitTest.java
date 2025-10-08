package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;

public class PathMatcherUnitTest extends AbstractJupiterUnitTest {

    @Test
    public void testMatcher() {
        AntPathMatcher matcher = new AntPathMatcher();
        String url = "/config/tenants/XM/entity/specs/xmentityspecs.yml";
        String pattern = "/config/tenants/{tenantName}/entity/specs/xmentityspecs.yml";

        Assertions.assertTrue(matcher.match(pattern, url));
        Assertions.assertEquals("XM", matcher.extractUriTemplateVariables(pattern, url).get("tenantName"));
    }

}
