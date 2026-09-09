package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.TypedQuery;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Binds template parameters (supplied + subject) and executes RAW templates. */
@Component
@RequiredArgsConstructor
public class JpqlTemplateExecutor {

    public static final String SUBJECT_USER_KEY = "subjectUserKey";
    public static final String SUBJECT_LOGIN = "subjectLogin";
    public static final String SUBJECT_TENANT = "subjectTenant";
    public static final Set<String> SUBJECT_PARAMS = Set.of(SUBJECT_USER_KEY, SUBJECT_LOGIN, SUBJECT_TENANT);

    private final EntityManager em;
    private final XmAuthenticationContextHolder authContextHolder;
    private final TenantContextHolder tenantContextHolder;

    public record RawResult(List<Map<String, Object>> rows, Long total) {
    }

    /** Returns exactly the params the template references: supplied values plus subject params. */
    public Map<String, Object> bindParams(JpqlTemplate template, Map<String, Object> supplied) {
        Map<String, Object> bound = new HashMap<>();
        for (String name : template.paramNames()) {
            if (supplied.containsKey(name)) {
                bound.put(name, supplied.get(name));
            } else if (SUBJECT_PARAMS.contains(name)) {
                Object value = subjectValue(name);
                if (value == null) {
                    throw new BusinessException(ERR_VALIDATION, "Subject param is not available for current user: " + name);
                }
                bound.put(name, value);
            } else {
                throw new BusinessException(ERR_VALIDATION, "Template param is required: " + name);
            }
        }
        return bound;
    }

    public RawResult executeRaw(JpqlTemplate template, Map<String, Object> params, Pageable pageable,
                                Function<Object, Object> entityToDto) {
        if (pageable != null && pageable.getSort().isSorted()) {
            throw new BusinessException(ERR_VALIDATION, "sort is not supported for RAW templates, use order by in the template");
        }
        Map<String, Object> bound = bindParams(template, params);

        TypedQuery<Tuple> query = em.createQuery(template.getQuery(), Tuple.class);
        bind(query, bound);
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        List<Map<String, Object>> rows = query.getResultList().stream().map(t -> toRow(t, entityToDto)).toList();

        Long total = null;
        if (StringUtils.isNotBlank(template.getCountQuery())) {
            TypedQuery<Long> count = em.createQuery(template.getCountQuery(), Long.class);
            bind(count, bound);
            total = count.getSingleResult();
        }
        return new RawResult(rows, total);
    }

    private static void bind(TypedQuery<?> query, Map<String, Object> bound) {
        query.getParameters().forEach(p -> {
            if (p.getName() != null && bound.containsKey(p.getName())) {
                query.setParameter(p.getName(), bound.get(p.getName()));
            }
        });
    }

    private static Map<String, Object> toRow(Tuple tuple, Function<Object, Object> entityToDto) {
        Map<String, Object> row = new LinkedHashMap<>();
        List<TupleElement<?>> elements = tuple.getElements();
        for (int i = 0; i < elements.size(); i++) {
            String alias = elements.get(i).getAlias();
            String key = StringUtils.isBlank(alias) ? "col" + i : alias;
            row.put(key, entityToDto.apply(tuple.get(i)));
        }
        return row;
    }

    private Object subjectValue(String name) {
        return switch (name) {
            case SUBJECT_USER_KEY -> authContextHolder.getContext().getUserKey().orElse(null);
            case SUBJECT_LOGIN -> authContextHolder.getContext().getLogin().orElse(null);
            case SUBJECT_TENANT -> TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
            default -> null;
        };
    }
}
