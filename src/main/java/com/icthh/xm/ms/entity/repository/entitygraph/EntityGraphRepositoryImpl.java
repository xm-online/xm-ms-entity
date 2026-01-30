package com.icthh.xm.ms.entity.repository.entitygraph;

import com.icthh.xm.ms.entity.domain.XmEntity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Query;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.icthh.xm.ms.entity.repository.entitygraph.JpaHints.LOAD_GRAPH;

@Slf4j
public class EntityGraphRepositoryImpl<T, I extends Serializable>
    extends SimpleJpaRepository<T, I> implements EntityGraphRepository<T, I> {

    private static final String GRAPH_DELIMETER = ".";
    private final EntityManager entityManager;

    private final Class<T> domainClass;

    public EntityGraphRepositoryImpl(JpaEntityInformation<T, I> entityInformation,
                                     EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
    }

    public EntityGraphRepositoryImpl(Class<T> domainClass,
                                     EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
        this.domainClass = domainClass;
    }

    @Override
    public T findOne(I id, List<String> embed) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = builder.createQuery(domainClass);
        Root<T> root = criteriaQuery.from(domainClass);
        criteriaQuery.where(builder.equal(root.get("id"), id));

        TypedQuery<T> query = entityManager
            .createQuery(criteriaQuery)
            .setHint(LOAD_GRAPH, createEntityGraph(embed));

        List<T> resultList = query.getResultList();
        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(0);
    }

    @Override
    public List<T> findAll(String jpql, Map<String, Object> args, List<String> embed) {
        Query query = entityManager.createQuery(jpql);
        args.forEach(query::setParameter);
        query.setHint(LOAD_GRAPH, createEntityGraph(embed));
        return query.getResultList();
    }

    @Override
    public List<?> findAll(String jpql, Map<String, Object> args) {
        Query query = entityManager.createQuery(jpql);
        args.forEach(query::setParameter);
        return query.getResultList();
    }

    @Override
    public List<?> findAll(String jpql, Map<String, Object> args, Pageable pageable) {
        Query query = entityManager.createQuery(jpql);
        args.forEach(query::setParameter);
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        return query.getResultList();
    }

    @Override
    public List<Tuple> findAll(Specification<T> spec, Function<Root<T>, List<Selection<?>>> fields, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
        Root<T> root = query.from(domainClass);
        query.multiselect(fields.apply(root));
        query.where(spec.toPredicate(root, query, criteriaBuilder));
        query.orderBy(QueryUtils.toOrders(pageable.getSort(), root, criteriaBuilder));

        TypedQuery<Tuple> typedQuery = entityManager.createQuery(query);
        if (pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }

        return typedQuery.getResultList();
    }

    public Long getSequenceNextValString(String sequenceName) {
        Dialect dialect = getDialect(entityManager);
        Query query = entityManager.createNativeQuery(dialect.getSequenceSupport().getSequenceNextValString(sequenceName));
        return Long.parseLong(query.getSingleResult().toString());
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        entityManager.setFlushMode(flushMode);
    }

    @Override
    public int update(Function<CriteriaBuilder, CriteriaUpdate<XmEntity>> criteriaUpdate) {
        CriteriaUpdate<XmEntity> updateQuery = criteriaUpdate.apply(entityManager.getCriteriaBuilder());
        return entityManager.createQuery(updateQuery).executeUpdate();
    }

    @Override
    public int delete(Function<CriteriaBuilder, CriteriaDelete<XmEntity>> criteriaDelete) {
        CriteriaDelete<XmEntity> deleteQuery = criteriaDelete.apply(entityManager.getCriteriaBuilder());
        return entityManager.createQuery(deleteQuery).executeUpdate();
    }

    private EntityGraph<T> createEntityGraph(List<String> embed) {
        EntityGraph<T> graph = entityManager.createEntityGraph(domainClass);
        if (CollectionUtils.isNotEmpty(embed)) {
            embed.forEach(f -> addAttributeNodes(f, graph));
        }
        return graph;
    }

    private static void addAttributeNodes(String fieldName, EntityGraph<?> graph) {
        int pos = fieldName.indexOf(GRAPH_DELIMETER);
        if (pos < 0) {
            graph.addAttributeNodes(fieldName);
            return;
        }

        String subgraphName = fieldName.substring(0, pos);
        Subgraph<?> subGraph = graph.addSubgraph(subgraphName);
        String nextFieldName = fieldName.substring(pos + 1);
        pos = nextFieldName.indexOf(GRAPH_DELIMETER);

        while (pos > 0) {
            subgraphName = nextFieldName.substring(0, pos);
            subGraph = subGraph.addSubgraph(subgraphName);
            nextFieldName = nextFieldName.substring(pos + 1);
            pos = nextFieldName.indexOf(GRAPH_DELIMETER);
        }

        subGraph.addAttributeNodes(nextFieldName);
    }

    private Dialect getDialect(EntityManager entityManager) {
        Session session = entityManager.unwrap(Session.class);
        SessionFactory sessionFactory = session.getSessionFactory();
        return ((SessionFactoryImplementor) sessionFactory).getJdbcServices().getDialect();
    }

}
