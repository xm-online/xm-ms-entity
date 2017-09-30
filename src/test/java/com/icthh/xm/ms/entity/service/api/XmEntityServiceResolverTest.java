package com.icthh.xm.ms.entity.service.api;

import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.service.XmEntityServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class XmEntityServiceResolverTest {

    @Autowired
    private XmEntityServiceResolver xmEntityService;

    @Test
    @Transactional
    public void testXmEntityServiceResolverDefault() throws Exception {

        TenantContext.setCurrent("RESINTTEST");

        String cName = xmEntityService.getService().getClass().getName();
        cName = StringUtils.substringBefore(cName, "$$");

        assertThat(cName).isEqualTo(XmEntityServiceImpl.class.getCanonicalName());

    }
}
