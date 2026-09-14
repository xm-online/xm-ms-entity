package com.icthh.xm.ms.entity.service.search.db.template;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.repository.search.db.QueryParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaSelection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Executes RAW templates: full JPQL returning rows as maps keyed by selection alias (or {@code colN}).
 *
 * <p>The template is parsed into a criteria query, so a sort from the request is added as criteria orders over
 * the selection items instead of being concatenated into the JPQL. Sort properties are therefore the aliases the
 * caller sees in the row, and an unknown one is a client error. A sort from the request replaces the
 * {@code order by} of the template; without one the template keeps its own ordering.
 */
@Component
@RequiredArgsConstructor
public class JpqlTemplateExecutor {

    private final EntityManager em;

    public record RawResult(List<Map<String, Object>> rows, Long total) {
    }

    public RawResult executeRaw(JpqlTemplate template, Map<String, Object> params, Pageable pageable,
                                Function<Object, Object> entityToDto) {
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
        JpaCriteriaQuery<Tuple> criteria = cb.createQuery(template.getQuery(), Tuple.class);
        List<JpaSelection<?>> selections = selections(criteria);
        if (pageable != null && pageable.getSort().isSorted()) {
            criteria.orderBy(orders(cb, selections, pageable.getSort()));
        }

        TypedQuery<?> query = em.createQuery(criteria);
        QueryParams.bind(query, params);
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        List<String> keys = keys(selections);
        List<Map<String, Object>> rows = query.getResultList().stream()
            .map(row -> toRow(row, keys, entityToDto))
            .toList();
        return new RawResult(rows, count(template, params));
    }

    /** A template selecting one thing has a plain selection, several make a compound one. */
    private static List<JpaSelection<?>> selections(JpaCriteriaQuery<?> criteria) {
        JpaSelection<?> selection = criteria.getSelection();
        return selection.isCompoundSelection() ? new ArrayList<>(selection.getSelectionItems()) : List.of(selection);
    }

    /** Row keys: the selection alias where the template declares one, the position otherwise. */
    private static List<String> keys(List<JpaSelection<?>> selections) {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < selections.size(); i++) {
            String alias = selections.get(i).getAlias();
            keys.add(isNotBlank(alias) ? alias : "col" + i);
        }
        return keys;
    }

    private static List<Order> orders(HibernateCriteriaBuilder cb, List<JpaSelection<?>> selections, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            Expression<?> expression = sortExpression(selections, order.getProperty());
            orders.add(order.isAscending() ? cb.asc(expression) : cb.desc(expression));
        }
        return orders;
    }

    /** Only an aliased selection can be sorted by: its alias is the key the caller gets back in the row. */
    private static Expression<?> sortExpression(List<JpaSelection<?>> selections, String property) {
        for (JpaSelection<?> selection : selections) {
            if (property.equals(selection.getAlias()) && selection instanceof Expression<?> expression) {
                return expression;
            }
        }
        throw new BusinessException(ERR_VALIDATION, "Unknown sort property for RAW template: " + property
            + ", expected one of " + sortable(selections));
    }

    private static List<String> sortable(List<JpaSelection<?>> selections) {
        return selections.stream()
            .filter(selection -> isNotBlank(selection.getAlias()) && selection instanceof Expression<?>)
            .map(JpaSelection::getAlias)
            .toList();
    }

    private Long count(JpqlTemplate template, Map<String, Object> params) {
        if (isBlank(template.getCountQuery())) {
            return null;
        }
        TypedQuery<Long> count = em.createQuery(template.getCountQuery(), Long.class);
        QueryParams.bind(count, params);
        return count.getSingleResult();
    }

    /** A single selection arrives as the value itself, several as an array. */
    private static Map<String, Object> toRow(Object row, List<String> keys, Function<Object, Object> entityToDto) {
        Object[] values = row instanceof Object[] array ? array : new Object[]{row};
        Map<String, Object> mapped = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            mapped.put(i < keys.size() ? keys.get(i) : "col" + i, entityToDto.apply(values[i]));
        }
        return mapped;
    }
}
