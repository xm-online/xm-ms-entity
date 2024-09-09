package com.icthh.xm.ms.entity.config.jsonb;

import com.icthh.xm.commons.migration.db.jsonb.JsonbExpression;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class JsonbCriteriaBuilder {

    private final CriteriaBuilder criteriaBuilder;
    private final JsonbExpression jsonbExpression;

    public JsonbCriteriaBuilder(CriteriaBuilder criteriaBuilder, JsonbExpression jsonbExpression) {
        this.criteriaBuilder = criteriaBuilder;
        this.jsonbExpression = jsonbExpression;
    }

    public Predicate equal(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.equal(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath),
            jsonbExpression.toJsonB(criteriaBuilder, object));
    }

    public Predicate equal(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.equal(
            jsonbExpression.jsonQuery(criteriaBuilder, xRoot, XmEntity_.DATA, xJsonPath),
            jsonbExpression.jsonQuery(criteriaBuilder, yRoot, XmEntity_.DATA, yJsonPath));
    }

    public Predicate equal(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.equal(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath),
            jsonbExpression.toJsonB(criteriaBuilder, expression));
    }

    public Predicate equalText(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.equal(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath),
            jsonbExpression.toJsonbText(criteriaBuilder, object));
    }

    public Predicate notEqual(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.notEqual(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath),
            jsonbExpression.toJsonB(criteriaBuilder, object));
    }

    public Predicate notEqual(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.notEqual(
            jsonbExpression.jsonQuery(criteriaBuilder, xRoot, XmEntity_.DATA, xJsonPath),
            jsonbExpression.jsonQuery(criteriaBuilder, yRoot, XmEntity_.DATA, yJsonPath));
    }

    public Predicate notEqual(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.notEqual(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath),
            jsonbExpression.toJsonB(criteriaBuilder, expression));
    }

    public Predicate notEqualText(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.notEqual(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath),
            jsonbExpression.toJsonbText(criteriaBuilder, object));
    }

    public Predicate greaterThanText(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.greaterThan(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonbText(criteriaBuilder, object, String.class));
    }

    public Predicate greaterThan(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.greaterThan(
            jsonbExpression.jsonQuery(criteriaBuilder, xRoot, XmEntity_.DATA, xJsonPath, String.class),
            jsonbExpression.jsonQuery(criteriaBuilder, yRoot, XmEntity_.DATA, yJsonPath, String.class));
    }

    public Predicate greaterThan(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.greaterThan(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate greaterThan(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.greaterThan(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate greaterThanOrEqualTo(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.greaterThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate greaterThanOrEqualTo(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot,
        String yJsonPath) {
        return criteriaBuilder.greaterThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, xRoot, XmEntity_.DATA, xJsonPath, String.class),
            jsonbExpression.jsonQuery(criteriaBuilder, yRoot, XmEntity_.DATA, yJsonPath, String.class));
    }

    public Predicate greaterThanOrEqualTo(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.greaterThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate greaterThanOrEqualToText(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.greaterThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonbText(criteriaBuilder, object, String.class));
    }

    public Predicate lessThan(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.lessThan(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate lessThan(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.lessThan(
            jsonbExpression.jsonQuery(criteriaBuilder, xRoot, XmEntity_.DATA, xJsonPath, String.class),
            jsonbExpression.jsonQuery(criteriaBuilder, yRoot, XmEntity_.DATA, yJsonPath, String.class));
    }

    public Predicate lessThan(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.lessThan(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate lessThanOrEqualTo(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.lessThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, object, String.class));
    }

    public Predicate lessThanText(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.lessThan(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonbText(criteriaBuilder, object, String.class));
    }

    public Predicate lessThanOrEqualTo(Root<XmEntity> xRoot, String xJsonPath, Root<XmEntity> yRoot, String yJsonPath) {
        return criteriaBuilder.lessThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, xRoot, XmEntity_.DATA, xJsonPath, String.class),
            jsonbExpression.jsonQuery(criteriaBuilder, yRoot, XmEntity_.DATA, yJsonPath, String.class));
    }

    public Predicate lessThanOrEqualTo(Root<XmEntity> root, String jsonPath, Expression<?> expression) {
        return criteriaBuilder.lessThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonB(criteriaBuilder, expression, String.class));
    }

    public Predicate lessThanOrEqualToText(Root<XmEntity> root, String jsonPath, Object object) {
        return criteriaBuilder.lessThanOrEqualTo(
            jsonbExpression.jsonQuery(criteriaBuilder, root, XmEntity_.DATA, jsonPath, String.class),
            jsonbExpression.toJsonbText(criteriaBuilder, object, String.class));
    }

}
