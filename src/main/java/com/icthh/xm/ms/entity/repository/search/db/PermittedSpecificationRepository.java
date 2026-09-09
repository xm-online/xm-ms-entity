package com.icthh.xm.ms.entity.repository.search.db;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

/**
 * Executes a JPA {@link Specification} together with the xm-commons row-level permission condition.
 * The permission SpEL is translated to JPQL by xm-commons (alias {@code returnObject}), the alias is
 * rewritten to {@value #ALIAS}, the resulting HQL is parsed into a criteria query and the specification
 * predicate is ANDed onto it.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class PermittedSpecificationRepository {

    public static final String ALIAS = "entity";
    private static final String COMMONS_ALIAS = "returnObject";

    private final EntityManager em;
    private final PermissionCheckService permissionCheckService;
    private final SpelToJpqlTranslator spelToJpqlTranslator = new SpelToJpqlTranslator();

    public <T> Page<T> findAll(Class<T> entityClass, Specification<T> spec, OrderProvider<T> orders,
                               Pageable pageable, String privilegeKey) {
        return findAll(entityClass, null, Map.of(), spec, orders, pageable, privilegeKey);
    }

    public <T> Page<T> findAll(Class<T> entityClass, String whereFragment, Map<String, Object> params,
                               Specification<T> spec, OrderProvider<T> orders, Pageable pageable,
                               String privilegeKey) {
        String where = buildWhere(whereFragment, privilegeKey);
        String entityName = em.getMetamodel().entity(entityClass).getName();
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();

        JpaCriteriaQuery<T> select = cb.createQuery(
            "select " + ALIAS + " from " + entityName + " " + ALIAS + where, entityClass);
        Root<T> root = singleRoot(select);
        applySpec(select, root, spec, cb);
        if (orders != null) {
            select.orderBy(orders.orders(root, cb));
        }
        TypedQuery<T> selectQuery = em.createQuery(select);
        bind(selectQuery, params);
        if (pageable != null && pageable.isPaged()) {
            selectQuery.setFirstResult((int) pageable.getOffset());
            selectQuery.setMaxResults(pageable.getPageSize());
        }
        log.debug("Executing DB search '{}' with params {}", select, params);
        List<T> content = selectQuery.getResultList();

        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(content);
        }
        JpaCriteriaQuery<Long> count = cb.createQuery(
            "select count(" + ALIAS + ") from " + entityName + " " + ALIAS + where, Long.class);
        applySpec(count, singleRoot(count), spec, cb);
        TypedQuery<Long> countQuery = em.createQuery(count);
        bind(countQuery, params);
        long total = countQuery.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private String buildWhere(String whereFragment, String privilegeKey) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(whereFragment)) {
            parts.add("(" + whereFragment + ")");
        }
        if (privilegeKey != null) {
            String condition = permissionCheckService.createCondition(
                SecurityContextHolder.getContext().getAuthentication(), privilegeKey, spelToJpqlTranslator);
            if (StringUtils.isNotBlank(condition)) {
                parts.add("(" + condition.replace(COMMONS_ALIAS, ALIAS) + ")");
            }
        }
        return parts.isEmpty() ? "" : " where " + String.join(" and ", parts);
    }

    @SuppressWarnings("unchecked")
    private static <T> Root<T> singleRoot(JpaCriteriaQuery<?> query) {
        return (Root<T>) query.getRoots().iterator().next();
    }

    private static <T> void applySpec(JpaCriteriaQuery<?> query, Root<T> root, Specification<T> spec,
                                      HibernateCriteriaBuilder cb) {
        if (spec == null) {
            return;
        }
        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate == null) {
            return;
        }
        Predicate existing = query.getRestriction();
        query.where(existing == null ? predicate : cb.and(existing, predicate));
    }

    private static void bind(TypedQuery<?> query, Map<String, Object> params) {
        query.getParameters().forEach(p -> {
            if (p.getName() != null && params.containsKey(p.getName())) {
                query.setParameter(p.getName(), params.get(p.getName()));
            }
        });
    }
}
