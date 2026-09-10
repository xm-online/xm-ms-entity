package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.repository.search.db.QueryParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.TypedQuery;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Executes RAW templates: full JPQL returning rows as maps keyed by selection alias (or {@code colN}). */
@Component
@RequiredArgsConstructor
public class JpqlTemplateExecutor {

    private final EntityManager em;

    public record RawResult(List<Map<String, Object>> rows, Long total) {
    }

    public RawResult executeRaw(JpqlTemplate template, Map<String, Object> params, Pageable pageable,
                                Function<Object, Object> entityToDto) {
        if (pageable != null && pageable.getSort().isSorted()) {
            throw new BusinessException(ERR_VALIDATION, "sort is not supported for RAW templates, use order by in the template");
        }
        TypedQuery<Tuple> query = em.createQuery(template.getQuery(), Tuple.class);
        QueryParams.bind(query, params);
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        List<Map<String, Object>> rows = query.getResultList().stream().map(tuple -> toRow(tuple, entityToDto)).toList();
        return new RawResult(rows, count(template, params));
    }

    private Long count(JpqlTemplate template, Map<String, Object> params) {
        if (isBlank(template.getCountQuery())) {
            return null;
        }
        TypedQuery<Long> count = em.createQuery(template.getCountQuery(), Long.class);
        QueryParams.bind(count, params);
        return count.getSingleResult();
    }

    private static Map<String, Object> toRow(Tuple tuple, Function<Object, Object> entityToDto) {
        Map<String, Object> row = new LinkedHashMap<>();
        List<TupleElement<?>> elements = tuple.getElements();
        for (int i = 0; i < elements.size(); i++) {
            String alias = elements.get(i).getAlias();
            row.put(isNotBlank(alias) ? alias : "col" + i, entityToDto.apply(tuple.get(i)));
        }
        return row;
    }
}
