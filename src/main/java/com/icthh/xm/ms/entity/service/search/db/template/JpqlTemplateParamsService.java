package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.lep.keyresolver.JpqlTemplateKeyResolver;
import com.icthh.xm.ms.entity.service.TransactionPropagationService;
import com.icthh.xm.ms.entity.service.search.db.filter.BooleanValues;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the named parameters of a JPQL template: request params (typed per template config) plus
 * subject params from the security context. Every step is a LEP so tenants can enrich or override params.
 */
@Service
@LepService(group = "service.entity.dbsearch")
@RequiredArgsConstructor
public class JpqlTemplateParamsService extends TransactionPropagationService<JpqlTemplateParamsService> {

    public static final String SUBJECT_USER_KEY = "subjectUserKey";
    public static final String SUBJECT_LOGIN = "subjectLogin";
    public static final String SUBJECT_TENANT = "subjectTenant";

    private final XmEntityJpqlTemplatesService templatesService;
    private final XmAuthenticationContextHolder authContextHolder;
    private final TenantContextHolder tenantContextHolder;

    /** Request params joined with subject params; a subject param overwrites a request param of the same name. */
    @LogicExtensionPoint(value = "GetTemplateParams", resolver = JpqlTemplateKeyResolver.class)
    public Map<String, Object> getParams(String templateKey, Map<String, Object> requestParams) {
        Map<String, Object> params = new HashMap<>(self.getRequestParams(templateKey, requestParams));
        params.putAll(self.getTemplateSubjectParams(templateKey));
        return params;
    }

    /** Client-supplied params converted to the types declared in the template. */
    @LogicExtensionPoint(value = "GetTemplateRequestParams", resolver = JpqlTemplateKeyResolver.class)
    public Map<String, Object> getRequestParams(String templateKey, Map<String, Object> requestParams) {
        return applyParamTypes(templatesService.getTemplate(templateKey), requestParams);
    }

    /** Subject params of one template; override it per template key, or {@link #getSubjectParams()} for all. */
    @LogicExtensionPoint(value = "GetTemplateSubjectParams", resolver = JpqlTemplateKeyResolver.class)
    public Map<String, Object> getTemplateSubjectParams(String templateKey) {
        return self.getSubjectParams();
    }

    /** {@code subjectUserKey}, {@code subjectLogin} (when present) and {@code subjectTenant}, for every template. */
    @LogicExtensionPoint("GetSubjectParams")
    public Map<String, Object> getSubjectParams() {
        XmAuthenticationContext auth = authContextHolder.getContext();
        Map<String, Object> subject = new HashMap<>();
        auth.getUserKey().ifPresent(userKey -> subject.put(SUBJECT_USER_KEY, userKey));
        auth.getLogin().ifPresent(login -> subject.put(SUBJECT_LOGIN, login));
        subject.put(SUBJECT_TENANT, getRequiredTenantKeyValue(tenantContextHolder));
        return subject;
    }

    /** Converts String values (GET query params) to the types declared in {@link JpqlTemplate#getParams()}. */
    public Map<String, Object> applyParamTypes(JpqlTemplate template, Map<String, ?> params) {
        Map<String, String> types = template.getParams() == null ? Map.of() : template.getParams();
        Map<String, Object> result = new HashMap<>();
        params.forEach((name, value) -> result.put(name, convert(name, types.get(name), value)));
        return result;
    }

    private static Object convert(String name, String type, Object value) {
        if (type == null || !(value instanceof String text)) {
            return value;
        }
        try {
            return switch (type) {
                case "number" -> text.contains(".") ? (Object) Double.valueOf(text) : (Object) Long.valueOf(text);
                case "boolean" -> BooleanValues.parse(name, text);
                case "instant" -> Instant.parse(text);
                case "list" -> Arrays.stream(text.split(",")).map(String::trim).toList();
                default -> text;
            };
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new BusinessException(ERR_VALIDATION, "Invalid value for template param " + name + ": " + value);
        }
    }
}
