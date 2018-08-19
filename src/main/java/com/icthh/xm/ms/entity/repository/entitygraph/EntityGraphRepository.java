package com.icthh.xm.ms.entity.repository.entitygraph;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@NoRepositoryBean
public interface EntityGraphRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    T findOne(ID id, List<String> embed);

    @Transactional(readOnly = true)
    List<T> findAll(String jpql, Map<String, Object> args, List<String> embed);
}
