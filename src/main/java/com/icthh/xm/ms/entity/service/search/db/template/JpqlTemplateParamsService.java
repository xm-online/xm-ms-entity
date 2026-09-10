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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public static final Set<String> SUBJECT_PARAMS = Set.of(SUBJECT_USER_KEY, SUBJECT_LOGIN, SUBJECT_TENANT);

    private final XmEntityJpqlTemplatesService templatesService;
    private final XmAuthenticationContextHolder authContextHolder;
    private final TenantContextHolder tenantContextHolder;

    /** Request params joined with subject params; subject params always win. */
    @LogicExtensionPoint(value = "GetTemplateParams", resolver = JpqlTemplateKeyResolver.class)
    public Map<String, Object> getParams(String templateKey, Map<String, Object> requestParams) {
        Map<String, Object> params = new HashMap<>(self.getRequestParams(templateKey, requestParams));
        params.putAll(self.getSubjectParams(templateKey));
        return params;
    }

    /** Client-supplied params converted to the types declared in the template; subject names are rejected. */
    @LogicExtensionPoint(value = "GetTemplateRequestParams", resolver = JpqlTemplateKeyResolver.class)
    public Map<String, Object> getRequestParams(String templateKey, Map<String, Object> requestParams) {
        requestParams.keySet().stream().filter(SUBJECT_PARAMS::contains).findFirst().ifPresent(name -> {
            throw new BusinessException(ERR_VALIDATION, "Subject param can not be supplied by client: " + name);
        });
        return templatesService.applyParamTypes(templatesService.getTemplate(templateKey), requestParams);
    }

    /** {@code subjectUserKey}, {@code subjectLogin} (when present) and {@code subjectTenant}. */
    @LogicExtensionPoint(value = "GetTemplateSubjectParams", resolver = JpqlTemplateKeyResolver.class)
    public Map<String, Object> getSubjectParams(String templateKey) {
        XmAuthenticationContext auth = authContextHolder.getContext();
        Map<String, Object> subject = new HashMap<>();
        auth.getUserKey().ifPresent(userKey -> subject.put(SUBJECT_USER_KEY, userKey));
        auth.getLogin().ifPresent(login -> subject.put(SUBJECT_LOGIN, login));
        subject.put(SUBJECT_TENANT, getRequiredTenantKeyValue(tenantContextHolder));
        return subject;
    }
}
