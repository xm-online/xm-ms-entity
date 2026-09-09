package com.icthh.xm.ms.entity.repository.search.db;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.List;

@FunctionalInterface
public interface OrderProvider<T> {
    List<Order> orders(Root<T> root, CriteriaBuilder cb);
}
