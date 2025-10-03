package com.icthh.xm.ms.entity.service.mail;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;

import static java.util.Locale.ENGLISH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@ActiveProfiles(profiles = "non-async")
public class MailServiceIntTest extends AbstractSpringBootTest {

    private static final String MAIL_SETTINGS = "mailSettings";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String EMAIL = "email";
    private static final String FROM = "from";
    private static final String RID = "rid";
    public static final String TENANT_NAME = "RESINTTEST";

    @SpyBean
    private MailService mailService;

    @Autowired
    private TenantEmailTemplateService templateService;

    @MockBean
    private JavaMailSender javaMailSender;

    /**
     * periodicMetricsTaskScheduler configures in @Profile("!non-async"), needed for test passing
     */
    @MockBean
    ThreadPoolTaskScheduler periodicMetricsTaskScheduler;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @SneakyThrows
    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_NAME);
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));
    }

    @Test
    public void testComplexTemplateEmail() throws InterruptedException {

        String mainPath = "/config/tenants/" + TENANT_NAME + "/entity/emails/en/" + TEMPLATE_NAME + ".ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/entity/emails/en/" + TEMPLATE_NAME + "-BASE.ftl";
        String body = "<#import \"/" + TENANT_NAME + "/en/" + TEMPLATE_NAME + "-BASE\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";
        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);
        mailService.sendEmailFromTemplate(TenantKey.valueOf(TENANT_NAME), ENGLISH, TEMPLATE_NAME, SUBJECT, EMAIL, Map.of(
                "variable1", "value1",
                "variable2", "value2"
        ), RID, FROM);

        verify(mailService).sendEmail(any(), any(), eq("OTHER_BASE_START_CUSTOM_BASE_END"), any(), any(), any());
    }

}
