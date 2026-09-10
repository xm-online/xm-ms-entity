package com.icthh.xm.ms.entity.service.search.db;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.spel.support.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Builds the denormalized text stored in xm_entity.search_text: name, description, then the values of
 * {@link TypeSpec#getFullTextSearchDataFields()}. Each data field is a SpEL path evaluated against the
 * entity ({@code data.customer.city}); expressions are compiled once and cached.
 */
@Slf4j
@Component
public class SearchTextBuilder {

    private static final String DATA_PREFIX = "data.";
    private static final String SEPARATOR = "\n";
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final Map<String, Expression> expressions = new ConcurrentHashMap<>();

    public String build(TypeSpec spec, XmEntity entity) {
        if (spec == null || !Boolean.TRUE.equals(spec.getFullTextSearch())) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        addIfNotBlank(parts, entity.getName());
        addIfNotBlank(parts, entity.getDescription());
        List<String> fields = spec.getFullTextSearchDataFields();
        if (fields != null) {
            EvaluationContext context = SimpleEvaluationContext
                .forPropertyAccessors(new MapAccessor(), DataBindingPropertyAccessor.forReadOnlyAccess())
                .withRootObject(entity)
                .build();
            fields.stream().filter(StringUtils::isNotBlank).forEach(field -> addIfNotBlank(parts, evaluate(field, context)));
        }
        return String.join(SEPARATOR, parts);
    }

    private String evaluate(String field, EvaluationContext context) {
        String path = field.startsWith(DATA_PREFIX) ? field : DATA_PREFIX + field;
        Expression expression = expressions.computeIfAbsent(path, PARSER::parseExpression);
        try {
            Object value = expression.getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (EvaluationException e) {
            log.debug("fullTextSearchDataFields entry {} can not be resolved: {}", field, e.getMessage());
            return null;
        }
    }

    private static void addIfNotBlank(List<String> parts, String value) {
        if (isNotBlank(value)) {
            parts.add(value);
        }
    }
}
