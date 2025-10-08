package com.icthh.xm.ms.entity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.service.mail.MailService;
import com.icthh.xm.ms.entity.service.mail.TenantEmailTemplateService;
import freemarker.template.Configuration;
import jakarta.activation.DataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.ms.entity.config.Constants.TRANSLATION_KEY;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRANCE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unused")
public class MailServiceUnitTest extends AbstractJupiterUnitTest {

    private static final String MAIL_SETTINGS = "mailSettings";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String FROM = "from";

    private static final String EMAIL_TEMPLATE = "EMAIL_TEMPLATE";
    private static final String TEST_TEMPLATE_CONTENT = "test template content";
    private static final String TENANT_KEY = "TENANT_KEY";
    private static final String MOCK_FROM = "MOCK_FROM";
    private static final String MOCK_SUBJECT = "MOCK_SUBJECT";
    private static final String TO = "to@yopmail.com";
    private static final String FILE_NAME = "FILE_NAME.csv";
    private static final String FILE_NAME2 = "FILE_NAME2.csv";
    private static final String ATTACHMENT = "attachment";
    private static final String TEXT_CSV = "text/csv";
    private static final byte[] FILE_BYTE_ARRAY = FILE_NAME.getBytes();

    @InjectMocks
    private MailService mailService;

    private JavaMailSender javaMailSender = mock(JavaMailSender.class);

    @Spy
    private MailProviderService mailProviderService = new MailProviderService(javaMailSender);
    @Mock
    private TenantEmailTemplateService tenantEmailTemplateService;
    @Spy
    private Configuration freeMarkerConfiguration = new Configuration(Configuration.VERSION_2_3_0);
    @Mock
    private LocalizationMessageService localizationMessageService;
    private TenantContextHolder tenantContextHolder = mock(TenantContextHolder.class);
    @Spy
    private XmEntityTenantConfigService tenantConfigService;

    @BeforeEach
    public void before() {
        tenantConfigService = new XmEntityTenantConfigService(new XmConfigProperties(), tenantContextHolder);
        MockitoAnnotations.initMocks(this);
        when(tenantContextHolder.getTenantKey()).thenReturn("XM");
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(mock(PrivilegedTenantContext.class));
        TenantContext mock = mock(TenantContext.class);
        when(mock.getTenantKey()).thenReturn(java.util.Optional.of(TenantKey.valueOf("XM")));
        when(tenantContextHolder.getContext()).thenReturn(mock);
    }

    @Test
    @SneakyThrows
    public void ifNoConfigReturnDefault() {
        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigMailSettingsNoListReturnDefault() {
        prepareConfig(new HashMap<>() {{
            put(MAIL_SETTINGS, singletonList(new HashMap<>()));
        }});

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    private MimeMessage sendEmail() {
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(mock(PrivilegedTenantContext.class));
        when(tenantEmailTemplateService.getEmailTemplate(TENANT_KEY + "/" + FRANCE.getLanguage() + "/" + EMAIL_TEMPLATE)).thenReturn(TEST_TEMPLATE_CONTENT);
        MimeMessage mock = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mock);

        mailService.sendEmailFromTemplate(
            TenantKey.valueOf(TENANT_KEY),
            FRANCE,
            EMAIL_TEMPLATE,
            MOCK_SUBJECT,
            TO,
            emptyMap(),
            "rid",
            MOCK_FROM
        );
        return mock;
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoTemplateReturnDefault() {
        Map<String, Object> map = of(MAIL_SETTINGS, singletonList(of(
            TEMPLATE_NAME, "OTHER_TEMPLATE",
            SUBJECT, of(ENGLISH.getLanguage(), "otherSubject"),
            FROM, of(ENGLISH.getLanguage(), "otherFrom"))));

        prepareConfig(map);

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @SneakyThrows
    private void prepareConfig(Map<String, Object> map) {
        tenantConfigService.onRefresh("/config/tenants/XM/tenant-config.yml",
                                      new ObjectMapper(new YAMLFactory()).writeValueAsString(map));
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoFieldReturnDefault() {
        prepareConfig(of(MAIL_SETTINGS, singletonList(of(TEMPLATE_NAME, EMAIL_TEMPLATE))));

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigHasTranslationKeyReturnTranslationByKey() {
        when(localizationMessageService.getMessage("tr subject key")).thenReturn("subject value");
        when(localizationMessageService.getMessage("tr from key")).thenReturn("fromvalue (From value caption)");

        prepareConfig(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE,
                SUBJECT, of(TRANSLATION_KEY, "tr subject key", ENGLISH.getLanguage(), "en subject", FRANCE.getLanguage(), "fr subject"),
                FROM, of(TRANSLATION_KEY, "tr from key", ENGLISH.getLanguage(), "en from", FRANCE.getLanguage(), "frfrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("fromvalue (From value caption)")[0]));
        verify(mock).setSubject(eq("subject value"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoTranslationKeyReturnByLocale() {
        prepareConfig(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE,
                SUBJECT, of(ENGLISH.getLanguage(), "en subject", FRANCE.getLanguage(), "fr subject"),
                FROM, of(ENGLISH.getLanguage(), "en from", FRANCE.getLanguage(), "frfrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("frfrom")[0]));
        verify(mock).setSubject(eq("fr subject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void ifInConfigNoTranslationKeyAndNoTranslationsByLocaleReturnEn() {
        prepareConfig(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, EMAIL_TEMPLATE,
                SUBJECT, of(ENGLISH.getLanguage(), "en subject"),
                FROM, of(ENGLISH.getLanguage(), "enfrom")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("enfrom")[0]));
        verify(mock).setSubject(eq("en subject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testSubjectConfiguration() {
        prepareConfig(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "EMAIL_TEMPLATE",
                SUBJECT, of(FRANCE.getLanguage(), "otherSubject")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq("otherSubject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testFromConfiguration() {
        prepareConfig(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "EMAIL_TEMPLATE",
                FROM, of(FRANCE.getLanguage(), "otherFrom@yopmail.com (France caption)")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("otherFrom@yopmail.com (France caption)")[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testSubjectAndFromConfiguration() {
        prepareConfig(
            of(MAIL_SETTINGS, singletonList(of(
                TEMPLATE_NAME, "EMAIL_TEMPLATE",
                SUBJECT, of(FRANCE.getLanguage(), "otherSubject"),
                FROM, of(FRANCE.getLanguage(), "otherFrom@yopmail.com (France caption)")
            )))
        );

        MimeMessage mock = sendEmail();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse("otherFrom@yopmail.com (France caption)")[0]));
        verify(mock).setSubject(eq("otherSubject"), eq("UTF-8"));

        verify(javaMailSender).send(mock);
    }

    @Test
    @SneakyThrows
    public void testSendEmailWithAttachment()  {
        prepareConfig(new HashMap<>() {{
            put(MAIL_SETTINGS, singletonList(new HashMap<>()));
        }});

        MimeMessage mock = sendEmailWithAttachment();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        ArgumentCaptor<Multipart> captor = ArgumentCaptor.forClass(Multipart.class);
        verify(mock).setContent(captor.capture());
        List<Multipart> multiparts = captor.getAllValues();
        assertMultipart(multiparts);

        verify(javaMailSender).send(mock);
    }


    @Test
    @SneakyThrows
    public void testSendEmailWithAttachments()  {
        prepareConfig(new HashMap<>() {{
            put(MAIL_SETTINGS, singletonList(new HashMap<>()));
        }});

        MimeMessage mock = sendEmailWithAttachments();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        ArgumentCaptor<Multipart> captor = ArgumentCaptor.forClass(Multipart.class);
        verify(mock).setContent(captor.capture());
        List<Multipart> multiparts = captor.getAllValues();
        assertMultiparts(multiparts);

        verify(javaMailSender).send(mock);
    }

    private MimeMessage sendEmailWithAttachment() {
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(mock(PrivilegedTenantContext.class));
        when(tenantEmailTemplateService.getEmailTemplate(TENANT_KEY + "/" + FRANCE.getLanguage() + "/" + EMAIL_TEMPLATE)).thenReturn(TEST_TEMPLATE_CONTENT);
        MimeMessage mock = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mock);

        mailService.sendEmailFromTemplateWithAttachment(
            TenantKey.valueOf(TENANT_KEY),
            FRANCE,
            EMAIL_TEMPLATE,
            MOCK_SUBJECT,
            TO,
            emptyMap(),
            "rid",
            MOCK_FROM,
            FILE_NAME,
            new ByteArrayResource(FILE_BYTE_ARRAY)
        );
        return mock;
    }

    private MimeMessage sendEmailWithAttachments() {
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(mock(PrivilegedTenantContext.class));
        when(tenantEmailTemplateService.getEmailTemplate(TENANT_KEY + "/" + FRANCE.getLanguage() + "/" + EMAIL_TEMPLATE)).thenReturn(TEST_TEMPLATE_CONTENT);
        MimeMessage mock = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mock);

        Map<String, InputStreamSource> attachments = Map.of(
            FILE_NAME, new ByteArrayResource(FILE_BYTE_ARRAY),
            FILE_NAME2, new ByteArrayResource(FILE_BYTE_ARRAY)
        );
        mailService.sendEmailFromTemplateWithAttachments(
            TenantKey.valueOf(TENANT_KEY),
            FRANCE,
            EMAIL_TEMPLATE,
            MOCK_SUBJECT,
            TO,
            emptyMap(),
            "rid",
            MOCK_FROM,
            attachments
        );
        return mock;
    }

    private void assertMultipart(List<Multipart> multiparts) throws Exception {
        assertEquals(1, multiparts.size());

        Multipart multipart = multiparts.get(0);
        assertEquals(2, multipart.getCount());

        BodyPart bodyPart = multipart.getBodyPart(1);
        assertEquals(ATTACHMENT, bodyPart.getDisposition());
        assertEquals(FILE_NAME, bodyPart.getFileName());

        DataSource dataSource = bodyPart.getDataHandler().getDataSource();
        assertEquals(FILE_NAME, dataSource.getName());
        assertEquals(TEXT_CSV, dataSource.getContentType());

        byte[] arrayFromInputStream = IOUtils.toByteArray(dataSource.getInputStream());
        assertArrayEquals(FILE_BYTE_ARRAY, arrayFromInputStream);
    }

    private void assertMultiparts(List<Multipart> multiparts) throws Exception {
        assertEquals(1, multiparts.size());
        List<String> fileNames = Lists.newArrayList(FILE_NAME, FILE_NAME2);

        Multipart multipart = multiparts.get(0);
        assertEquals(3, multipart.getCount());
        for (int i = 1; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);

            assertEquals(ATTACHMENT, bodyPart.getDisposition());
            String fileName = bodyPart.getFileName();
            assertTrue(fileNames.remove(fileName));

            DataSource dataSource = bodyPart.getDataHandler().getDataSource();
            assertEquals(fileName, dataSource.getName());
            assertEquals(TEXT_CSV, dataSource.getContentType());

            byte[] arrayFromInputStream = IOUtils.toByteArray(dataSource.getInputStream());
            assertArrayEquals(FILE_BYTE_ARRAY, arrayFromInputStream);
        }
    }

    @Test
    @SneakyThrows
    public void testSendEmailWithContentAndAttachment()  {
        prepareConfig(new HashMap<>() {{
            put(MAIL_SETTINGS, singletonList(new HashMap<>()));
        }});

        MimeMessage mock = sendEmailWithContentAndAttachment();

        verify(mock).setRecipient(eq(Message.RecipientType.TO),  eq(InternetAddress.parse(TO)[0]));
        verify(mock).setFrom(eq(InternetAddress.parse(MOCK_FROM)[0]));
        verify(mock).setSubject(eq(MOCK_SUBJECT), eq("UTF-8"));

        ArgumentCaptor<Multipart> captor = ArgumentCaptor.forClass(Multipart.class);
        verify(mock).setContent(captor.capture());
        List<Multipart> multiparts = captor.getAllValues();
        assertMultipart(multiparts);

        verify(javaMailSender).send(mock);
    }

    private MimeMessage sendEmailWithContentAndAttachment() {
        MimeMessage mock = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mock);

        mailService.sendEmailWithContentAndAttachments(
            TenantKey.valueOf(TENANT_KEY),
            TEST_TEMPLATE_CONTENT,
            MOCK_SUBJECT,
            TO,
            MOCK_FROM,
            FILE_NAME,
            new ByteArrayResource(FILE_BYTE_ARRAY)
        );
        return mock;
    }

}
