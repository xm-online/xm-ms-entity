package com.icthh.xm.ms.entity.service.search.db.filter;

import static com.icthh.xm.commons.exceptions.ErrorConstants.ERR_VALIDATION;
import static java.util.stream.Collectors.toUnmodifiableSet;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import java.util.Set;
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
    private volatile Set<String> columns;

    public boolean isColumn(String field) {
        return columns().contains(field);
    }

    public void assertColumn(String field) {
        if (!isColumn(field)) {
            throw new BusinessException(ERR_VALIDATION, "Unknown filter field: " + field);
        }
    }

    private Set<String> columns() {
        if (columns == null) {
            columns = em.getMetamodel().entity(XmEntity.class).getSingularAttributes().stream()
                .map(Attribute::getName)
                .filter(name -> !XmEntity_.DATA.equals(name))
                .collect(toUnmodifiableSet());
        }
        return columns;
    }
}
