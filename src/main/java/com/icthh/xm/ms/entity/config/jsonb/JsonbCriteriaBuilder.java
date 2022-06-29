package com.icthh.xm.ms.entity.config.jsonb;

import com.icthh.xm.ms.entity.domain.XmEntity;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonbCriteriaBuilder {

    private final CriteriaBuilder criteriaBuilder;

    public Predicate equal(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.equal(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath),
            JsonbUtils.toJsonB(criteriaBuilder, object));
    }

    public Predicate equal(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.equal(
            JsonbUtils.jsonQuery(criteriaBuilder, xRoot, xJsonPath),
            JsonbUtils.jsonQuery(criteriaBuilder, yRoot, yJsonPath));
    }

    public Predicate equal(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.equal(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath),
            JsonbUtils.toJsonB(criteriaBuilder, expression));
    }

    public Predicate notEqual(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.notEqual(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath),
            JsonbUtils.toJsonB(criteriaBuilder, object));
    }

    public Predicate notEqual(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.notEqual(
            JsonbUtils.jsonQuery(criteriaBuilder, xRoot, xJsonPath),
            JsonbUtils.jsonQuery(criteriaBuilder, yRoot, yJsonPath));
    }

    public Predicate notEqual(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.notEqual(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath),
            JsonbUtils.toJsonB(criteriaBuilder, expression));
    }

    public Predicate greaterThan(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.greaterThan(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate greaterThan(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.greaterThan(
            JsonbUtils.jsonQuery(criteriaBuilder, xRoot, xJsonPath, String.class),
            JsonbUtils.jsonQuery(criteriaBuilder, yRoot, yJsonPath, String.class));
    }

    public Predicate greaterThan(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.greaterThan(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate greaterThanOrEqualTo(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.greaterThanOrEqualTo(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate greaterThanOrEqualTo(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot,
        String yJsonPath) {
        return criteriaBuilder.greaterThanOrEqualTo(
            JsonbUtils.jsonQuery(criteriaBuilder, xRoot, xJsonPath, String.class),
            JsonbUtils.jsonQuery(criteriaBuilder, yRoot, yJsonPath, String.class));
    }

    public Predicate greaterThanOrEqualTo(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.greaterThanOrEqualTo(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate lessThan(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.lessThan(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate lessThan(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.lessThan(
            JsonbUtils.jsonQuery(criteriaBuilder, xRoot, xJsonPath, String.class),
            JsonbUtils.jsonQuery(criteriaBuilder, yRoot, yJsonPath, String.class));
    }

    public Predicate lessThan(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.lessThan(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate lessThanOrEqualTo(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.lessThanOrEqualTo(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate lessThanOrEqualTo(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.lessThanOrEqualTo(
            JsonbUtils.jsonQuery(criteriaBuilder, xRoot, xJsonPath, String.class),
            JsonbUtils.jsonQuery(criteriaBuilder, yRoot, yJsonPath, String.class));
    }

    public Predicate lessThanOrEqualTo(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.lessThanOrEqualTo(
            JsonbUtils.jsonQuery(criteriaBuilder, root, jsonPath, String.class),
            JsonbUtils.toJsonB(criteriaBuilder, expression, String.class));
    }

}
