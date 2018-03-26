package com.icthh.xm.ms.entity.service.mail;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.jhipster.config.JHipsterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.CharEncoding;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

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
    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;
    private final TenantEmailTemplateService tenantEmailTemplateService;
    private final Configuration freeMarkerConfiguration;
    private final TenantContextHolder tenantContextHolder;

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
                Template mailTemplate = new Template(templateKey, emailTemplate, freeMarkerConfiguration);
                String content = FreeMarkerTemplateUtils.processTemplateIntoString(mailTemplate, objectModel);
                sendEmail(email, subject, content, from);
            } catch (TemplateException e) {
                throw new IllegalStateException("Mail template rendering failed");
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading mail template");
            }
        });
    }

    private String generateFrom(TenantKey tenantKey) {
        return jHipsterProperties.getMail().getFrom().replace("<tenantname>", tenantKey.getValue());
    }

    // package level for testing
    void sendEmail(String to, String subject, String content, String from) {
        log.debug("Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            false, true, to, subject, content);

        // Prepare message using a Spring helper
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, CharEncoding.UTF_8);
            message.setTo(to);
            message.setFrom(from);
            message.setSubject(subject);
            message.setText(content, true);
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
