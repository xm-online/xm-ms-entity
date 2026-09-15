package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Filterable / sortable XmEntity fields: every persisted single-valued attribute except {@code data},
 * which is addressed through {@code data.<path>}. Derived from the JPA metamodel, so new columns are
 * picked up automatically.
 */
@Component
@RequiredArgsConstructor
public class XmEntityColumns {

    private final EntityManager em;
    private volatile Map<String, SingularAttribute<? super XmEntity, ?>> attributes;

    public boolean isColumn(String field) {
        return attributes().containsKey(field);
    }

    /** Metamodel attribute of a filterable / sortable field; 400 when the field is not one. */
    public SingularAttribute<? super XmEntity, ?> attribute(String field) {
        SingularAttribute<? super XmEntity, ?> attribute = attributes().get(field);
        if (attribute == null) {
            throw new BusinessException(ERR_VALIDATION, "Unknown filter field: " + field);
        }
        return attribute;
    }

    public Set<String> columns() {
        return attributes().keySet().stream().collect(toUnmodifiableSet());
    }

    private Map<String, SingularAttribute<? super XmEntity, ?>> attributes() {
        if (attributes == null) {
            attributes = em.getMetamodel().entity(XmEntity.class).getSingularAttributes().stream()
                .filter(attribute -> !XmEntity_.DATA.equals(attribute.getName()))
                .collect(Collectors.toUnmodifiableMap(SingularAttribute::getName, Function.identity()));
        }
        return attributes;
    }
}
