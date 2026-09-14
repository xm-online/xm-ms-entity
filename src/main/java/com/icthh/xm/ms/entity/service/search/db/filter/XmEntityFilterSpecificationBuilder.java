package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.search.db.dialect.JsonValueStrategy;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Turns parsed {@link FilterCondition}s into JPA {@link Specification}s. {@code entityPath} resolves the XmEntity
 * from the query root (identity for XmEntity queries, {@code root.get("target")} for Link queries).
 */
@Component
@RequiredArgsConstructor
public class XmEntityFilterSpecificationBuilder {

    private static final char ESCAPE = '\\';

    private final JsonValueStrategy jsonValueStrategy;
    private final XmEntityColumns columns;
    private final XmEntitySpecService xmEntitySpecService;

    public Specification<XmEntity> build(List<FilterCondition> conditions) {
        return build(conditions, root -> root);
    }

    public <T> Specification<T> build(List<FilterCondition> conditions, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<XmEntity> entity = entityPath.apply(root);
            List<Predicate> predicates = new ArrayList<>();
            for (FilterCondition condition : conditions) {
                predicates.add(condition.isDataField()
                    ? dataPredicate(cb, entity, condition)
                    : columnPredicate(cb, entity, condition));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Subtypes are matched by {@code in} over the type keys declared in the spec, not by a {@code like} on the
     * column, so the type_key index is used. A prefix with no concrete type below it matches nothing.
     */
    public <T> Specification<T> typeKey(String typeKey, boolean includeSubTypes, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<String> path = entityPath.apply(root).get(XmEntity_.typeKey);
            if (!includeSubTypes) {
                return cb.equal(path, typeKey);
            }
            List<String> typeKeys = subTypeKeys(typeKey);
            return typeKeys.isEmpty() ? cb.disjunction() : path.in(typeKeys);
        };
    }

    private List<String> subTypeKeys(String typeKey) {
        return xmEntitySpecService.findNonAbstractTypesByPrefix(typeKey).stream().map(TypeSpec::getKey).toList();
    }

    public <T> Specification<T> notRemoved(Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<Boolean> removed = entityPath.apply(root).get(XmEntity_.removed);
            return cb.or(cb.isNull(removed), cb.isFalse(removed));
        };
    }

    public <T> Specification<T> fullText(String query, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, cq, cb) -> ilike(cb, entityPath.apply(root).get(XmEntity_.searchText),
            "%" + escapeLike(query) + "%");
    }

    /**
     * Only {@code removed.eq=true} opts into soft-deleted rows; any other {@code removed.*} filter keeps the
     * default "not removed" predicate, so a deleted row can never leak through, for example, {@code removed.specified}.
     */
    public static boolean includesRemoved(List<FilterCondition> conditions) {
        return conditions.stream().anyMatch(c -> XmEntity_.REMOVED.equals(c.field())
            && c.operator() == FilterOperator.EQ
            && Boolean.TRUE.equals(c.singleValue()));
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Predicate ilike(CriteriaBuilder cb, Expression<String> text, String pattern) {
        return ((HibernateCriteriaBuilder) cb).ilike(text, pattern, ESCAPE);
    }

    /** {@code contains} matches anywhere, {@code startsWith} and {@code endsWith} anchor one side. */
    private static String likePattern(FilterOperator operator, Object value) {
        String escaped = escapeLike(String.valueOf(value));
        return switch (operator) {
            case STARTS_WITH -> escaped + "%";
            case ENDS_WITH -> "%" + escaped;
            default -> "%" + escaped + "%";
        };
    }

    // ---- data.* ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate dataPredicate(CriteriaBuilder cb, Path<XmEntity> entity, FilterCondition c) {
        Path<?> data = entity.get(XmEntity_.data);
        String jsonPath = c.dataJsonPath();
        // the compared value drives the SQL type on dialects that extract json as scalars (Oracle)
        Expression value = jsonValueStrategy.jsonValue(cb, data, jsonPath, c.singleValue());
        return switch (c.operator()) {
            case EQ -> cb.equal(value, jsonValueStrategy.literal(cb, c.singleValue()));
            case NOT_EQ -> cb.notEqual(value, jsonValueStrategy.literal(cb, c.singleValue()));
            case IN -> value.in(c.values().stream().map(v -> jsonValueStrategy.literal(cb, v)).toList());
            case NOT_IN -> cb.not(value.in(c.values().stream().map(v -> jsonValueStrategy.literal(cb, v)).toList()));
            case CONTAINS, STARTS_WITH, ENDS_WITH -> ilike(cb, jsonValueStrategy.jsonText(cb, data, jsonPath),
                likePattern(c.operator(), c.singleValue()));
            case HAS -> jsonValueStrategy.arrayHas(cb, data, jsonPath, c.singleValue());
            case SPECIFIED -> Boolean.TRUE.equals(c.singleValue()) ? cb.isNotNull(value) : cb.isNull(value);
            case GT -> cb.greaterThan(value, (Expression) jsonValueStrategy.literal(cb, c.singleValue()));
            case GTE -> cb.greaterThanOrEqualTo(value, (Expression) jsonValueStrategy.literal(cb, c.singleValue()));
            case LT -> cb.lessThan(value, (Expression) jsonValueStrategy.literal(cb, c.singleValue()));
            case LTE -> cb.lessThanOrEqualTo(value, (Expression) jsonValueStrategy.literal(cb, c.singleValue()));
        };
    }

    // ---- columns ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate columnPredicate(CriteriaBuilder cb, Path<XmEntity> entity, FilterCondition c) {
        Path path = entity.get(columns.attribute(c.field()));
        Class<?> javaType = path.getJavaType();
        List<Object> values = c.values().stream().map(v -> convert(c.field(), javaType, v)).toList();
        Object value = values.isEmpty() ? null : values.get(0);
        return switch (c.operator()) {
            case EQ -> cb.equal(path, value);
            case NOT_EQ -> cb.notEqual(path, value);
            case IN -> path.in(values);
            case NOT_IN -> cb.not(path.in(values));
            case CONTAINS, STARTS_WITH, ENDS_WITH -> ilike(cb, path.as(String.class),
                likePattern(c.operator(), c.singleValue()));
            case HAS -> throw new BusinessException(ERR_VALIDATION,
                "Filter operator has is supported for data fields only: " + c.field());
            case SPECIFIED -> Boolean.TRUE.equals(c.singleValue()) ? cb.isNotNull(path) : cb.isNull(path);
            case GT -> cb.greaterThan(path, (Comparable) value);
            case GTE -> cb.greaterThanOrEqualTo(path, (Comparable) value);
            case LT -> cb.lessThan(path, (Comparable) value);
            case LTE -> cb.lessThanOrEqualTo(path, (Comparable) value);
        };
    }

    static Object convert(String field, Class<?> javaType, Object value) {
        try {
            if (value == null || javaType.isInstance(value)) {
                return value;
            }
            String s = String.valueOf(value);
            if (javaType == Long.class) {
                return Long.valueOf(s);
            }
            if (javaType == Integer.class) {
                return Integer.valueOf(s);
            }
            if (javaType == Boolean.class) {
                return BooleanValues.parse(field, s);
            }
            if (javaType == Instant.class) {
                return Instant.parse(s);
            }
            return s;
        } catch (NumberFormatException | DateTimeParseException e) {
            throw new BusinessException(ERR_VALIDATION, "Invalid value for filter field " + field + ": " + value);
        }
    }
}
