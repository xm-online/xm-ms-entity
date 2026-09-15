package com.icthh.xm.ms.entity.repository.search.db;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.List;

/**
 * Builds the {@code order by} part of a criteria query for {@link PermittedSpecificationRepository}.
 * The repository parses the permission HQL into a criteria query first, so the ordering has to be
 * expressed against that query's own root: the provider receives the root and returns the orders.
 * Used to sort by entity columns and by jsonb paths ({@code data.<path>}) in one place.
 */
@FunctionalInterface
public interface OrderProvider<T> {
    List<Order> orders(Root<T> root, CriteriaBuilder cb);
}
