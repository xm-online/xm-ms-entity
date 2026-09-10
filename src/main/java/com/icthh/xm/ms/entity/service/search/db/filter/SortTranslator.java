package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.search.db.OrderProvider;
import com.icthh.xm.ms.entity.service.search.db.dialect.JsonValueStrategy;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Translates a Spring {@link Sort} into criteria {@link jakarta.persistence.criteria.Order}s. Allowed properties are
 * the whitelisted XmEntity columns ({@link XmEntityFilterSpecificationBuilder#COLUMN_FIELDS}) and {@code data.<path>}
 * json paths resolved through {@link JsonValueStrategy}. {@code entityPath} resolves the XmEntity from the query root
 * (identity for XmEntity queries, {@code root.get("target")} for Link queries).
 */
@Component
@RequiredArgsConstructor
public class SortTranslator {

    private static final Pattern DATA_PATH = Pattern.compile("^data(\\.[A-Za-z0-9_]+)+$");

    private final JsonValueStrategy jsonValueStrategy;
    private final XmEntityColumns columns;

    public OrderProvider<XmEntity> toOrderProvider(Sort sort) {
        return toOrderProvider(sort, root -> root);
    }

    public <T> OrderProvider<T> toOrderProvider(Sort sort, Function<Root<T>, Path<XmEntity>> entityPath) {
        validate(sort);
        return (root, cb) -> {
            Path<XmEntity> entity = entityPath.apply(root);
            return sort.stream().map(order -> {
                Expression<?> expression = order.getProperty().startsWith(FilterCondition.DATA_PREFIX)
                    ? jsonValueStrategy.jsonValue(cb, entity.get(XmEntity_.data),
                        "$." + order.getProperty().substring(FilterCondition.DATA_PREFIX.length()))
                    : entity.get(order.getProperty());
                return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
            }).toList();
        };
    }

    public void validate(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            boolean column = columns.isColumn(property);
            boolean dataPath = DATA_PATH.matcher(property).matches();
            if (!column && !dataPath) {
                throw new BusinessException(ERR_VALIDATION, "Unknown sort property: " + property);
            }
        }
    }
}
