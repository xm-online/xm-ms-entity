package com.icthh.xm.ms.entity.repository.entitygraph;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@NoRepositoryBean
public interface EntityGraphRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    T findOne(ID id, List<String> embed);

    List<T> findAll(String jpql, Map<String, Object> args, List<String> embed);
}
