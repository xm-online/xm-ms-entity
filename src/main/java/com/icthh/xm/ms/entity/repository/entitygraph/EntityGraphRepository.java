package com.icthh.xm.ms.entity.repository.entitygraph;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.FlushModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@NoRepositoryBean
public interface EntityGraphRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    T findOne(ID id, List<String> embed);

    List<T> findAll(String jpql, Map<String, Object> args, List<String> embed);

    List<?> findAll(String jpql, Map<String, Object> args);

    List<?> findAll(String jpql, Map<String, Object> args, Pageable pageable);

    Long getSequenceNextValString(String sequenceName);

    void setFlushMode(FlushModeType flushMode);

    int update(Function<CriteriaBuilder, CriteriaUpdate<XmEntity>> criteriaUpdate);

    int delete(Function<CriteriaBuilder, CriteriaDelete<XmEntity>> criteriaDelete);
}
