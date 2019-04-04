package com.icthh.xm.ms.entity.service.mail;

import static com.icthh.xm.ms.entity.config.Constants.TRANSLATION_KEY;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.nonNull;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;
import static org.springframework.context.i18n.LocaleContextHolder.getLocaleContext;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static org.springframework.context.i18n.LocaleContextHolder.setLocaleContext;

import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.PlainTenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
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
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
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

/**
 * Service for sending emails.
 * We use the @Async annotation to send emails asynchronously.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@IgnoreLogginAspect
public class MailService {

    private static final String MAIL_SETTINGS = "mailSettings";
    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String FROM = "from";

    private final JHipsterProperties jHipsterProperties;
    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final Configuration freeMarkerConfiguration;
    private final TenantContextHolder tenantContextHolder;
    private final TenantConfigService tenantConfigService;
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
        selfReference.sendEmailFromTemplateWithAttachment(
            tenantKey,
            locale,
            templateName,
            subject,
            email,
            objectModel,
            rid,
            from,
            null,
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
                sendEmail(
                    email,
                    resolve(SUBJECT, subject, templateName, locale),
                    content,
                    resolve(FROM, from, templateName, locale),
                    attachmentFilename,
                    dataSource
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

    private String resolve(String key, String defaultValue, String templateName, Locale locale) {
        Object settings = tenantConfigService.getConfig().get(MAIL_SETTINGS);
        LocaleContext localeContext = getLocaleContext();
        setLocale(locale);

        String result = Optional.ofNullable(settings)
            .filter(List.class::isInstance).map(it -> (List<Object>)it)
            .flatMap(mailSettings ->
                mailSettings.stream()
                    .filter(Map.class::isInstance).map(it -> (Map<String, Object>)it)
                    // find by templateName
                    .filter(it -> templateName.equals(it.get(TEMPLATE_NAME)))
                    .findFirst()
                    // get from of subject or etc it exists...
                    .filter(it -> it.containsKey(key))
                    .map(it -> it.get(key))
                    // get localized name
                    .filter(Map.class::isInstance).map(it -> (Map<String, String>)it)
                    .flatMap(this::getI18nName)
            ).orElse(defaultValue);

        setLocaleContext(localeContext);
        return result;
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
                   String attachmentFilename,
                   InputStreamSource dataSource) {

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper message;
        try {
            boolean hasAttachments = nonNull(attachmentFilename) || nonNull(dataSource);

            log.debug("Send email[multipart '{}' and html '{}' and attachmentFilename '{}'] to '{}' with subject '{}' and content={}",
                true, true, attachmentFilename, to, subject, content);

            message = new MimeMessageHelper(mimeMessage, hasAttachments, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(subject);
            message.setText(content, true);
            if (hasAttachments) {
                message.addAttachment(attachmentFilename, dataSource);
            }
            javaMailSender.send(mimeMessage);
            log.debug("Sent email to User '{}'", to);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Email could not be sent to user '{}'", to, e);
            } else {
                log.warn("Email could not be sent to user '{}': {}", to, e.getMessage());
            }
        }
    }
}
