package com.icthh.xm.ms.entity.config.jsonb;

import static com.icthh.xm.ms.entity.config.jsonb.CustomPostgreSQL95Dialect.JSON_QUERY;
import static com.icthh.xm.ms.entity.config.jsonb.CustomPostgreSQL95Dialect.TO_JSON_B;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import lombok.experimental.UtilityClass;

/**
 * Utility class for use custom sql json functions in {@link JsonbCriteriaBuilder}
 */
@UtilityClass
public class JsonbUtils {

    public Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<XmEntity> root, String jsonPath) {
        return jsonQuery(cb, root, jsonPath, JsonBinaryType.class);
    }

    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<XmEntity> root, String jsonPath, Class<T> type) {
        return cb.function(JSON_QUERY, type, root.get(XmEntity_.DATA), cb.literal(jsonPath));
    }

    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Object object) {
        return toJsonB(cb, object, JsonBinaryType.class);
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type) {
        return toJsonB(cb, cb.literal(object), type);
    }

    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        return toJsonB(cb, expression, JsonBinaryType.class);
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        return cb.function(TO_JSON_B, type, expression);
    }

}
