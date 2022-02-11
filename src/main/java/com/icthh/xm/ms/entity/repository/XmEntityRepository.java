package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import javax.persistence.FlushModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaUpdate;

/**
 * Repository interface accessible by LEP scripts.
 */
public interface XmEntityRepository {

    Optional<XmEntity> findById(Long id);

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use findById(Long aLong) instead.
     * </p>
     */
    @Deprecated
    XmEntity findOne(Long id);

    XmEntity findOne(Specification<XmEntity> spec);

    XmEntity findOne(Long id, List<String> embed);

    /**
     * For backward compatibility in LEPs.
     */
    @Deprecated
    <S extends XmEntity> S findOne(Example<S> example);

    XmEntity findOneById(Long id);

    XmEntity findOneByIdForUpdate(@Param("id") Long id);

    List<XmEntity> findAllById(Iterable<Long> longs);

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use findAllById(IterableIterable&lt;Long&gt; longs) instead.
     * </p>
     */
    @Deprecated
    List<XmEntity> findAll(Iterable<Long> longs);

    List<XmEntity> findAll(Specification<XmEntity> spec);

    Page<XmEntity> findAll(Specification<XmEntity> spec, Pageable pageable);

    List<XmEntity> findAll(Specification<XmEntity> spec, Sort sort);

    List<XmEntity> findAll(String jpql, Map<String, Object> args, List<String> embed);

    List<?> findAll(String jpql, Map<String, Object> args);

    List<?> findAll(String jpql, Map<String, Object> args, Pageable pageable);

    Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys);

    XmEntity findOneByKeyAndTypeKey(String key, String typeKey);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyByKey(String key);

    XmEntityIdKeyTypeKey findOneIdKeyTypeKeyById(Long id);

    XmEntityStateProjection findStateProjectionByKey(String key);

    XmEntityStateProjection findStateProjectionById(Long id);

    XmEntity getOne(Long id);

    <S extends XmEntity> S save(S entity);

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use saveAll(Iterable<? extends XmEntity> entities) instead.
     * </p>
     */
    @Deprecated
    <S extends XmEntity> List<S> save(Iterable<S> entities);

    <S extends XmEntity> S saveAndFlush(S entity);

    <S extends XmEntity> List<S> saveAll(Iterable<S> entities);

    boolean existsById(Long id);

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use existsById(Long id) instead.
     * </p>
     */
    @Deprecated
    boolean exists(Long id);

    void deleteById(Long id);

    void delete(XmEntity entity);

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use deleteById(Long id) instead.
     * </p>
     */
    @Deprecated
    void delete(Long id);

    /**
     * For backward compatibility in LEPs.
     * <p>
     * Deprecated: use deleteAll(Iterable&lt? extends XmEntity&gt) instead.
     * </p>
     */
    @Deprecated
    void delete(Iterable<? extends XmEntity> entities);

    void deleteAll(Iterable<? extends XmEntity> entities);

    Long getSequenceNextValString(String sequenceName);

    void deleteInBatch(Iterable<XmEntity> entities);

    void setFlushMode(FlushModeType flushMode);

    int update(Function<CriteriaBuilder, CriteriaUpdate<XmEntity>> criteriaUpdate);

    int delete(Function<CriteriaBuilder, CriteriaDelete<XmEntity>> criteriaDelete);

}
