package com.icthh.xm.ms.entity.repository.entitygraph;

import static org.hibernate.jpa.QueryHints.HINT_LOADGRAPH;

import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EntityGraphRepositoryImpl implements EntityGraphRepository {

    private static final String GRAPH_DELIMETER = ".";
    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public XmEntity findOne(Long id, List<String> embed) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<XmEntity> criteriaQuery = builder.createQuery(XmEntity.class);
        Root<XmEntity> root = criteriaQuery.from(XmEntity.class);
        criteriaQuery.where(builder.equal(root.get("id"), id));

        TypedQuery<XmEntity> query = entityManager
            .createQuery(criteriaQuery)
            .setHint(HINT_LOADGRAPH, createEntityGraph(embed));

        List<XmEntity> resultList = query.getResultList();
        if (CollectionUtils.isEmpty(resultList)) {
            return null;
        }
        return resultList.get(0);
    }

    @Override
    public List<XmEntity> findAll(String jpql, Map<String, Object> args, List<String> embed) {
        Query query = entityManager.createQuery(jpql);
        args.forEach(query::setParameter);
        query.setHint(HINT_LOADGRAPH, createEntityGraph(embed));
        return query.getResultList();
    }

    private EntityGraph<XmEntity> createEntityGraph(List<String> embed) {
        EntityGraph<XmEntity> graph = entityManager.createEntityGraph(XmEntity.class);
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

}
