package com.icthh.xm.ms.entity.service.search.db;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.service.spec.XmEntitySpecUpdatedEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.MapAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Single place that recomputes {@code xm_entity.search_text}, used by the JPA listener and by reindex.
 * The text is name, description, then the values of {@link TypeSpec#getFullTextSearchDataFields()}. Each data
 * field is a SpEL path evaluated against the entity ({@code data.customer.city}). A type without
 * {@code fullTextSearch} gets no text at all.
 *
 * <p>The expressions are parsed on {@link XmEntitySpecUpdatedEvent}, which the spec service publishes from
 * its {@code refreshFinished}, not while an entity is being saved, so a malformed path is reported once
 * against the config instead of on every persist. The cache is keyed by path and shared by all tenants: the
 * same path parses to the same expression.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchTextUpdater {

    private static final String DATA_PREFIX = "data.";
    private static final String SEPARATOR = "\n";
    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final XmEntitySpecService xmEntitySpecService;
    private final Map<String, Expression> expressions = new ConcurrentHashMap<>();

    /** Parses every configured data field of the refreshed spec, so saving an entity only evaluates. */
    @EventListener
    @IgnoreLogginAspect
    public void onSpecUpdated(XmEntitySpecUpdatedEvent event) {
        event.specs().values().stream()
            .map(TypeSpec::getFullTextSearchDataFields)
            .filter(fields -> fields != null)
            .flatMap(List::stream)
            .filter(StringUtils::isNotBlank)
            .forEach(field -> compile(field, event.tenantKey()));
    }

    public void refresh(XmEntity entity) {
        TypeSpec spec = xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(entity.getTypeKey()).orElse(null);
        entity.setSearchText(build(spec, entity));
    }

    private String build(TypeSpec spec, XmEntity entity) {
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
        Expression expression = expressions.get(path(field));
        if (expression == null) {
            // spec refreshed before this bean was listening, or the path did not parse
            expression = compile(field, null);
        }
        if (expression == null) {
            return null;
        }
        try {
            Object value = expression.getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (EvaluationException e) {
            log.debug("fullTextSearchDataFields entry {} can not be resolved: {}", field, e.getMessage());
            return null;
        }
    }

    private Expression compile(String field, String tenantKey) {
        String path = path(field);
        Expression cached = expressions.get(path);
        if (cached != null) {
            return cached;
        }
        try {
            Expression expression = PARSER.parseExpression(path);
            expressions.put(path, expression);
            return expression;
        } catch (ParseException e) {
            log.error("fullTextSearchDataFields entry {} of tenant {} is not a valid expression: {}",
                field, tenantKey, e.getMessage());
            return null;
        }
    }

    private static String path(String field) {
        return field.startsWith(DATA_PREFIX) ? field : DATA_PREFIX + field;
    }

    private static void addIfNotBlank(List<String> parts, String value) {
        if (isNotBlank(value)) {
            parts.add(value);
        }
    }
}
