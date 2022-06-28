package com.icthh.xm.ms.entity.repository.selection;

import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;

public interface EntitySelection<T> {

    CompoundSelection<T> buildSelection(Root<?> root, CriteriaBuilder cb, Class<T> projectionClass);
}
