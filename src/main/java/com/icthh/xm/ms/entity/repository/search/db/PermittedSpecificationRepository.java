package com.icthh.xm.ms.entity.repository.search.db;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    /** xm-commons alias only where it is used as a path root (followed by '.'), so literals stay untouched. */
    private static final Pattern COMMONS_ALIAS = Pattern.compile("\\breturnObject(?=\\.)");

    private final EntityManager em;
    private final PermissionCheckService permissionCheckService;
    private final SpelToJpqlTranslator spelToJpqlTranslator = new SpelToJpqlTranslator();

    public <T> Page<T> findAll(Class<T> entityClass, Specification<T> spec, OrderProvider<T> orders,
                               Pageable pageable, String privilegeKey) {
        return findAll(entityClass, spec, orders, pageable, privilegeKey, true);
    }

    /** @param countTotal {@code false} skips the count query; the page total is then unknown (offset + page size) */
    public <T> Page<T> findAll(Class<T> entityClass, Specification<T> spec, OrderProvider<T> orders,
                               Pageable pageable, String privilegeKey, boolean countTotal) {
        return findAll(entityClass, null, Map.of(), spec, orders, pageable, privilegeKey, countTotal);
    }

    /**
     * @param whereFragment optional JPQL condition over alias {@value #ALIAS} with named params
     * @param params        values for the named params of {@code whereFragment}
     * @param spec          optional criteria specification, ANDed to the parsed HQL
     * @param orders        optional ordering
     * @param privilegeKey  row-level privilege; {@code null} disables the permission condition
     */
    public <T> Page<T> findAll(Class<T> entityClass, String whereFragment, Map<String, Object> params,
                               Specification<T> spec, OrderProvider<T> orders, Pageable pageable,
                               String privilegeKey) {
        return findAll(entityClass, whereFragment, params, spec, orders, pageable, privilegeKey, true);
    }

    /** @param countTotal {@code false} skips the count query; the page total is then unknown (offset + page size) */
    public <T> Page<T> findAll(Class<T> entityClass, String whereFragment, Map<String, Object> params,
                               Specification<T> spec, OrderProvider<T> orders, Pageable pageable,
                               String privilegeKey, boolean countTotal) {
        String from = " from " + em.getMetamodel().entity(entityClass).getName() + " " + ALIAS
            + where(whereFragment, privilegeKey);

        List<T> content = createQuery("select " + ALIAS + from, entityClass, spec, orders, params, pageable)
            .getResultList();
        if (pageable == null || pageable.isUnpaged()) {
            return new PageImpl<>(content);
        }
        if (!countTotal) {
            return new PageImpl<>(content, pageable, pageable.getOffset() + content.size());
        }
        long total = createQuery("select count(" + ALIAS + ")" + from, Long.class, spec, null, params, null)
            .getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private <T, R> TypedQuery<R> createQuery(String hql, Class<R> resultClass, Specification<T> spec,
                                             OrderProvider<T> orders, Map<String, Object> params,
                                             Pageable pageable) {
        HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
        JpaCriteriaQuery<R> criteria = cb.createQuery(hql, resultClass);
        Root<T> root = singleRoot(criteria);
        applySpec(criteria, root, spec, cb);
        if (orders != null) {
            criteria.orderBy(orders.orders(root, cb));
        }
        TypedQuery<R> query = em.createQuery(criteria);
        QueryParams.bind(query, params);
        if (pageable != null && pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        log.debug("DB search query: {} params: {}", criteria, params);
        return query;
    }

    private String where(String whereFragment, String privilegeKey) {
        List<String> parts = new ArrayList<>();
        if (isNotBlank(whereFragment)) {
            parts.add("(" + whereFragment + ")");
        }
        String permission = permissionCondition(privilegeKey);
        if (isNotBlank(permission)) {
            parts.add("(" + COMMONS_ALIAS.matcher(permission).replaceAll(ALIAS) + ")");
        }
        return parts.isEmpty() ? "" : " where " + String.join(" and ", parts);
    }

    private String permissionCondition(String privilegeKey) {
        if (privilegeKey == null) {
            return null;
        }
        return permissionCheckService.createCondition(
            SecurityContextHolder.getContext().getAuthentication(), privilegeKey, spelToJpqlTranslator);
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
}
