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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Translates a Spring {@link Sort} into criteria {@link jakarta.persistence.criteria.Order}s. Allowed properties are
 * the persisted XmEntity columns ({@link XmEntityColumns}) and {@code data.<path>}
 * json paths resolved through {@link JsonValueStrategy}. {@code entityPath} resolves the XmEntity from the query root
 * (identity for XmEntity queries, {@code root.get("target")} for Link queries).
 */
@Component
@RequiredArgsConstructor
public class SortTranslator {

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
                // no operand: ordering has no compared value, so text-extracting dialects order lexically
                Expression<?> expression = DataPath.isDataPath(order.getProperty())
                    ? jsonValueStrategy.jsonValue(cb, entity.get(XmEntity_.data), DataPath.toJsonPath(order.getProperty()), null)
                    : entity.get(columns.attribute(order.getProperty()));
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
            if (!columns.isColumn(property) && !DataPath.isValid(property)) {
                throw new BusinessException(ERR_VALIDATION, "Unknown sort property: " + property);
            }
        }
    }
}
