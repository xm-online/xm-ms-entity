package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
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
import java.util.Set;
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

    public static final Set<String> COLUMN_FIELDS = Set.of(
        XmEntity_.ID, XmEntity_.KEY, XmEntity_.TYPE_KEY, XmEntity_.STATE_KEY, XmEntity_.NAME, XmEntity_.DESCRIPTION,
        XmEntity_.START_DATE, XmEntity_.UPDATE_DATE, XmEntity_.END_DATE, XmEntity_.CREATED_BY, XmEntity_.UPDATED_BY,
        XmEntity_.REMOVED);
    private static final char ESCAPE = '\\';

    private final JsonValueStrategy jsonValueStrategy;

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

    public <T> Specification<T> typeKey(String typeKey, boolean includeSubTypes, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<String> path = entityPath.apply(root).get(XmEntity_.typeKey);
            Predicate exact = cb.equal(path, typeKey);
            return includeSubTypes ? cb.or(exact, cb.like(path, typeKey + ".%")) : exact;
        };
    }

    public <T> Specification<T> notRemoved(Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, query, cb) -> {
            Path<Boolean> removed = entityPath.apply(root).get(XmEntity_.removed);
            return cb.or(cb.isNull(removed), cb.isFalse(removed));
        };
    }

    public <T> Specification<T> fullText(String query, Function<Root<T>, Path<XmEntity>> entityPath) {
        return (root, cq, cb) -> ((HibernateCriteriaBuilder) cb).ilike(
            entityPath.apply(root).get(XmEntity_.searchText), "%" + escapeLike(query) + "%", ESCAPE);
    }

    public static boolean hasRemovedCondition(List<FilterCondition> conditions) {
        return conditions.stream().anyMatch(c -> XmEntity_.REMOVED.equals(c.field()));
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ---- data.* ----

    private Predicate dataPredicate(CriteriaBuilder cb, Path<XmEntity> entity, FilterCondition c) {
        Path<?> data = entity.get(XmEntity_.data);
        String jsonPath = c.dataJsonPath();
        Expression<String> value = jsonValueStrategy.jsonValue(cb, data, jsonPath);
        return switch (c.operator()) {
            case EQ -> cb.equal(value, jsonValueStrategy.literal(cb, c.singleValue()));
            case NOT_EQ -> cb.notEqual(value, jsonValueStrategy.literal(cb, c.singleValue()));
            case IN -> value.in(c.values().stream().map(v -> jsonValueStrategy.literal(cb, v)).toList());
            case NOT_IN -> cb.not(value.in(c.values().stream().map(v -> jsonValueStrategy.literal(cb, v)).toList()));
            case CONTAINS -> ((HibernateCriteriaBuilder) cb).ilike(jsonValueStrategy.jsonText(cb, data, jsonPath),
                "%" + escapeLike(String.valueOf(c.singleValue())) + "%", ESCAPE);
            case SPECIFIED -> Boolean.TRUE.equals(c.singleValue()) ? cb.isNotNull(value) : cb.isNull(value);
            case GT -> cb.greaterThan(value, literalString(cb, c.singleValue()));
            case GTE -> cb.greaterThanOrEqualTo(value, literalString(cb, c.singleValue()));
            case LT -> cb.lessThan(value, literalString(cb, c.singleValue()));
            case LTE -> cb.lessThanOrEqualTo(value, literalString(cb, c.singleValue()));
        };
    }

    @SuppressWarnings("unchecked")
    private Expression<String> literalString(CriteriaBuilder cb, Object value) {
        // declared type is String because jsonValue is declared String; the DB compares jsonb (Postgres) or text (Oracle)
        return (Expression<String>) jsonValueStrategy.literal(cb, value);
    }

    // ---- columns ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate columnPredicate(CriteriaBuilder cb, Path<XmEntity> entity, FilterCondition c) {
        if (!COLUMN_FIELDS.contains(c.field())) {
            throw new BusinessException(ERR_VALIDATION, "Unknown filter field: " + c.field());
        }
        Path path = entity.get(c.field());
        Class<?> javaType = path.getJavaType();
        List<Object> values = c.values().stream().map(v -> convert(c.field(), javaType, v)).toList();
        Object value = values.isEmpty() ? null : values.get(0);
        return switch (c.operator()) {
            case EQ -> cb.equal(path, value);
            case NOT_EQ -> cb.notEqual(path, value);
            case IN -> path.in(values);
            case NOT_IN -> cb.not(path.in(values));
            case CONTAINS -> ((HibernateCriteriaBuilder) cb).ilike(path.as(String.class),
                "%" + escapeLike(String.valueOf(c.singleValue())) + "%", ESCAPE);
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
                return Boolean.valueOf(s);
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
