package com.icthh.xm.ms.entity.repository;

import com.icthh.xm.ms.entity.domain.XmEntity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.intellij.lang.annotations.Language;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Repository interface accessible by LEP scripts.
 */
public interface XmEntityRepository extends XmEntityProjectionRepository {

    Optional<XmEntity> findById(Long id);

    /**
     * For backward compatibility in LEPs.
     */
    XmEntity findOne(Long id);

    XmEntity findOne(Specification<XmEntity> spec);

    XmEntity findOne(Long id, List<String> embed);

    /**
     * For backward compatibility in LEPs.
     */
    @Deprecated
    <S extends XmEntity> S findOne(Example<S> example);

    /**
     * Returns entity by ID with using xmEntityGraph.
     * <p>
     * Deprecated: fetch all relations (like target, attachments, etc) use findById(Long aLong) instead.
     * </p>
     * @param id identifier of the entity
     * @return xmEntity instance
     */
    @Deprecated
    XmEntity findOneById(Long id);


    // PESSIMISTIC_WRITE locked query
    XmEntity findOneByIdForUpdate(Long id);

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

    List<Tuple> findAll(Specification<XmEntity> spec, Function<Root<XmEntity>, List<Selection<?>>> fields, Pageable pageable);

    List<XmEntity> findAll(@Language("HQL") String jpql, Map<String, Object> args, List<String> embed);

    List<?> findAll(@Language("HQL") String jpql, Map<String, Object> args);

    List<?> findAll(@Language("HQL") String jpql, Map<String, Object> args, Pageable pageable);

    Page<XmEntity> findAllByTypeKeyIn(Pageable pageable, Set<String> typeKeys);

    XmEntity findOneByKeyAndTypeKey(String key, String typeKey);

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

    long count();

    long count(Specification<XmEntity> spec);
}
