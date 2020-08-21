package com.icthh.xm.ms.entity.service.mail;

import static com.icthh.xm.ms.entity.config.Constants.TRANSLATION_KEY;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;
import static org.springframework.context.i18n.LocaleContextHolder.getLocaleContext;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static org.springframework.context.i18n.LocaleContextHolder.setLocaleContext;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.mail.provider.MailProviderService;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService;
import com.icthh.xm.ms.entity.config.XmEntityTenantConfigService.XmEntityTenantConfig.MailSetting;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.jhipster.config.JHipsterProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.CollectionUtils;

/**
 * Service for sending emails.
 * We use the @Async annotation to send emails asynchronously.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class MailService {

    private final JHipsterProperties jHipsterProperties;
    private final MailProviderService mailProviderService;
    private final MessageSource messageSource;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final Configuration freeMarkerConfiguration;
    private final TenantContextHolder tenantContextHolder;
    private final XmEntityTenantConfigService tenantConfigService;
    private final LocalizationMessageService localizationMessageService;

    @Resource
    @Lazy
    private MailService selfReference;

    private static void execForCustomRid(String rid, Runnable runnable) {
        final String oldRid = MdcUtils.getRid();
        try {
            MdcUtils.putRid(rid);
            runnable.run();
        } finally {
            if (oldRid != null) {
                MdcUtils.putRid(oldRid);
            } else {
                MdcUtils.removeRid();
            }
        }
    }

    /**
     * Send mail with raw subject and from.
     * @param locale the locale
     * @param templateName the email template name
     * @param subject the raw subject
     * @param email the to email
     * @param from the from email
     * @param objectModel the email parameters
     */
    public void sendEmailFromTemplate(Locale locale,
                                      String templateName,
                                      String subject,
                                      String email,
                                      String from,
                                      Map<String, Object> objectModel) {
        selfReference.sendEmailFromTemplate(TenantContextUtils.getRequiredTenantKey(tenantContextHolder.getContext()),
            locale,
            templateName,
            subject,
            email,
            objectModel,
            MdcUtils.generateRid(),
            from);
    }

    /**
     * Send email with subject from messageSource and generated from.
     * @param tenantKey the tenant key
     * @param locale the locale
     * @param templateName the email template name
     * @param titleKey the subject key from messageSource
     * @param email the to email
     * @param model the email parameters
     * @param rid the request id
     */
    public void sendEmailFromTemplate(TenantKey tenantKey,
                                      Locale locale,
                                      String templateName,
                                      String titleKey,
                                      String email,
                                      Map<String, Object> model,
                                      String rid) {
        String subject = messageSource.getMessage(titleKey, null, locale);
        selfReference.sendEmailFromTemplate(tenantKey, locale, templateName, subject, email, model, rid, generateFrom(tenantKey));
    }

    /**
     * Async send of email.
     * @param tenantKey the tenant key
     * @param locale the locale
     * @param templateName the email template name
     * @param subject the raw subject
     * @param email the to email
     * @param objectModel the email parameters
     * @param rid the request id
     * @param from the from email
     */
    @Async
    public void sendEmailFromTemplate(TenantKey tenantKey,
                               Locale locale,
                               String templateName,
                               String subject,
                               String email,
                               Map<String, Object> objectModel,
                               String rid,
                               String from) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject, email,
            objectModel,
            rid,
            from,
            null);
    }

    /**
     * Async send of email with attachment
     * @param tenantKey the tenant key
     * @param locale the locale
     * @param templateName the email template name
     * @param subject the raw subject
     * @param email the to email
     * @param objectModel the email parameters
     * @param rid the request id
     * @param from the from email
     * @param attachmentFilename the name of the attachment as it will appear in the mail
     * @param dataSource the {@code javax.activation.DataSource} to take the content from, determining the InputStream
     * and the content type
     */
    @Async
    public void sendEmailFromTemplateWithAttachment(TenantKey tenantKey,
                                      Locale locale,
                                      String templateName,
                                      String subject,
                                      String email,
                                      Map<String, Object> objectModel,
                                      String rid,
                                      String from,
                                      String attachmentFilename,
                                      InputStreamSource dataSource) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject,
            email,
            objectModel,
            rid,
            from,
            Map.of(attachmentFilename, dataSource));
    }

    /**
     * Async send of email with attachment
     * @param tenantKey the tenant key
     * @param locale the locale
     * @param templateName the email template name
     * @param subject the raw subject
     * @param email the to email
     * @param objectModel the email parameters
     * @param rid the request id
     * @param from the from email
     * @param attachments map of attachment file name which appear in the mail and data source of file content
     * and the content type
     */
    @Async
    public void sendEmailFromTemplateWithAttachments(TenantKey tenantKey,
                                                    Locale locale,
                                                    String templateName,
                                                    String subject,
                                                    String email,
                                                    Map<String, Object> objectModel,
                                                    String rid,
                                                    String from,
                                                    Map<String, InputStreamSource> attachments) {
        initAndSendEmail(tenantKey,
            locale,
            templateName,
            subject,
            email,
            objectModel,
            rid,
            from,
            attachments);
    }

    private void initAndSendEmail(TenantKey tenantKey,
                                  Locale locale,
                                  String templateName,
                                  String subject,
                                  String email,
                                  Map<String, Object> objectModel,
                                  String rid,
                                  String from,
                                  Map<String, InputStreamSource> attachments) {
        execForCustomRid(rid, () -> {
            if (email == null) {
                log.warn("Can't send email on null address for tenant: {}, email template: {}",
                    tenantKey.getValue(),
                    templateName);
                return;
            }

            String templateKey = EmailTemplateUtil.emailTemplateKey(tenantKey, locale.getLanguage(), templateName);
            String emailTemplate = tenantEmailTemplateService.getEmailTemplate(templateKey);

            try {
                tenantContextHolder.getPrivilegedContext().setTenant(new PlainTenant(tenantKey));

                Template mailTemplate = new Template(templateKey, emailTemplate, freeMarkerConfiguration);
                String content = FreeMarkerTemplateUtils.processTemplateIntoString(mailTemplate, objectModel);
                MailParams mailParams = resolve(subject, from, templateName, locale);
                sendEmail(
                    email,
                    mailParams.getSubject(),
                    content,
                    mailParams.getFrom(),
                    attachments,
                    mailProviderService.getJavaMailSender(tenantKey.getValue())
                );
            } catch (TemplateException e) {
                throw new IllegalStateException("Mail template rendering failed");
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading mail template");
            } finally {
                tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
            }
        });
    }

    private String generateFrom(TenantKey tenantKey) {
        return jHipsterProperties.getMail().getFrom().replace("<tenantname>", tenantKey.getValue());
    }

    private MailParams resolve(String subject, String from, String templateName, Locale locale) {
        MailParams mailParams = new MailParams(subject, from);

        List<MailSetting> settings = tenantConfigService.getXmEntityTenantConfig().getMailSettings();
        LocaleContext localeContext = getLocaleContext();
        setLocale(locale);

        mailParams = settings.stream().filter(it -> templateName.equals(it.getTemplateName()))
                .findFirst()
                .map(mailSetting -> new MailParams(getI18nName(mailSetting.getSubject()).orElse(subject),
                                                   getI18nName(mailSetting.getFrom()).orElse(from)))
                .orElse(mailParams);

        setLocaleContext(localeContext);
        return mailParams;
    }

    private Optional<String> getI18nName(Map<String, String> name) {
        if (name.containsKey(TRANSLATION_KEY)) {
            String translationKey = name.get(TRANSLATION_KEY);
            return Optional.of(localizationMessageService.getMessage(translationKey));
        } else if (name.containsKey(getLocale().getLanguage())) {
            return Optional.of(name.get(getLocale().getLanguage()));
        } else if (name.containsKey(ENGLISH.getLanguage())) {
            return Optional.of(name.get(ENGLISH.getLanguage()));
        }
        return Optional.empty();
    }

    // package level for testing
    void sendEmail(String to,
                   String subject,
                   String content,
                   String from,
                   Map<String, InputStreamSource> attachments,
                   JavaMailSender javaMailSender) {

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            boolean hasAttachments = !isEmpty(attachments) &&
                attachments
                    .entrySet()
                    .stream()
                    .allMatch(entry -> nonNull(entry.getKey()) && nonNull(entry.getValue()));

            log.debug("Send email[multipart '{}' and html '{}' and attachmentFilenames '{}' to '{}' with subject '{}' and content={}",
                hasAttachments, true, ofNullable(attachments).map(Map::keySet).orElse(null), to, subject, content);

            message = new MimeMessageHelper(mimeMessage, hasAttachments, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(subject);
            message.setText(content, true);
            if (hasAttachments) {
                for (Map.Entry<String, InputStreamSource> entry : attachments.entrySet()) {
                    message.addAttachment(entry.getKey(), entry.getValue());
                }
            }
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Email could not be sent to user '{}'", to, e);
            } else {
                log.warn("Email could not be sent to user '{}': {}", to, e.getMessage());
            }
        }
    }

    /**
     * Send mail with raw subject, from and content.
     *
     * @param content     the content of email
     * @param subject     the raw subject
     * @param email       the to email
     * @param from        the from email
     */
    @Async
    public void sendEmailWithContent(
        TenantKey tenantKey,
        String content,
        String subject,
        String email,
        String from) {
        initAndSendEmail(tenantKey,
            content,
            subject,
            email,
            MdcUtils.generateRid(),
            from,
            null);
    }

    /**
     * Send mail with raw subject, from and content.
     *
     * @param content     the content of email
     * @param subject     the raw subject
     * @param email       the to email
     * @param from        the from email
     * @param attachmentFilename the name of the attachment as it will appear in the mail
     * @param dataSource the {@code javax.activation.DataSource} to take the content from, determining the InputStream
     * and the content type
     */
    @Async
    public void sendEmailWithContentAndAttachments(
        TenantKey tenantKey,
        String content,
        String subject,
        String email,
        String from,
        String attachmentFilename,
        InputStreamSource dataSource) {
        initAndSendEmail(tenantKey,
            content,
            subject,
            email,
            MdcUtils.generateRid(),
            from,
            Map.of(attachmentFilename, dataSource));
    }

    private void initAndSendEmail(TenantKey tenantKey,
                                  String content,
                                  String subject,
                                  String email,
                                  String rid,
                                  String from,
                                  Map<String, InputStreamSource> attachments) {
        execForCustomRid(rid, () -> {
            if (email == null) {
                log.warn("Can't send email on null address for tenant: {}, Email [ subject : {}, to : {} ]",
                    tenantKey.getValue(), subject, email);
                return;
            }

            try {
                tenantContextHolder.getPrivilegedContext().setTenant(new PlainTenant(tenantKey));
                sendEmail(
                    email,
                    subject,
                    content,
                    from,
                    attachments,
                    mailProviderService.getJavaMailSender(tenantKey.getValue())
                );
            } finally {
                tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
            }
        });
    }

    @Data
    private static class MailParams {
        private String subject;
        private String from;
        MailParams(String subject, String from) {
            this.subject = subject;
            this.from = from;
        }
    }
}
