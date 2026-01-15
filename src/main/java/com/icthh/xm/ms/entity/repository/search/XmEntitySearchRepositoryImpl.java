package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.commons.search.builder.QueryBuilder;
import com.icthh.xm.commons.search.query.SearchQuery;
import com.icthh.xm.commons.search.query.dto.DeleteQuery;
import com.icthh.xm.commons.search.query.dto.GetQuery;
import com.icthh.xm.commons.search.query.dto.IndexQuery;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.icthh.xm.commons.search.builder.QueryBuilders.matchAllQuery;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "application.elastic-enabled", havingValue = "true", matchIfMissing = true)
public class XmEntitySearchRepositoryImpl implements XmEntitySearchRepository {

    private final TenantContextHolder tenantContextHolder;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public <S extends XmEntity> S index(S entity) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Iterable<XmEntity> search(QueryBuilder query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Page<XmEntity> search(QueryBuilder query, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Page<XmEntity> search(SearchQuery searchQuery) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Page<XmEntity> searchSimilar(XmEntity entity, String[] fields, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void refresh() {
        String indexName = elasticsearchOperations.composeIndexName(tenantContextHolder.getTenantKey());
        elasticsearchOperations.refresh(indexName);
    }

    @Override
    public Class<XmEntity> getEntityClass() {
        return XmEntity.class;
    }

    @Override
    public Iterable<XmEntity> findAll(Sort sort) {
        int itemCount = (int) this.count();
        if (itemCount == 0) {
            return new PageImpl<>(Collections.<XmEntity> emptyList());
        }
        SearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .withPageable(PageRequest.of(0, itemCount, sort)).build();
        return elasticsearchOperations.queryForPage(query, getEntityClass());
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable) {
        SearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .withIndices(elasticsearchOperations.getIndexName())
            .withTypes(ElasticsearchOperations.INDEX_QUERY_TYPE)
            .withPageable(pageable)
            .build();
        return elasticsearchOperations.queryForPage(query, getEntityClass());
    }

    @Override
    public <S extends XmEntity> S save(S entity) {
        String indexName = elasticsearchOperations.composeIndexName(tenantContextHolder.getTenantKey());
        elasticsearchOperations.index(createIndexQuery(indexName, entity));
        return entity;
    }

    @Override
    public <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities) {
        String indexName = elasticsearchOperations.composeIndexName(tenantContextHolder.getTenantKey());

        List<IndexQuery> queries = new ArrayList<>();
        for (S s : entities) {
            queries.add(createIndexQuery(indexName, s));
        }

        elasticsearchOperations.bulkIndex(queries);
        elasticsearchOperations.refresh(indexName);
        return entities;
    }

    private <S extends XmEntity> IndexQuery createIndexQuery(String indexName, S entity) {
        IndexQuery query = new IndexQuery();
        query.setObject(entity);
        query.setId(String.valueOf(entity.getId()));
        query.setType(ElasticsearchOperations.INDEX_QUERY_TYPE);
        query.setIndexName(indexName);
        return query;
    }


    @Override
    public Optional<XmEntity> findById(Long id) {
        GetQuery query = new GetQuery();
        query.setId(String.valueOf(id));
        return Optional.ofNullable(elasticsearchOperations.queryForObject(query, getEntityClass()));
    }

    @Override
    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Override
    public Iterable<XmEntity> findAll() {
        int itemCount = (int) this.count();
        if (itemCount == 0) {
            return new PageImpl<>(Collections.<XmEntity> emptyList());
        }
        return this.findAll(PageRequest.of(0, Math.max(1, itemCount)));
    }

    @Override
    public Iterable<XmEntity> findAllById(Iterable<Long> longs) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long count() {
        SearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(matchAllQuery())
            .withIndices(elasticsearchOperations.getIndexName())
            .withTypes(ElasticsearchOperations.INDEX_QUERY_TYPE)
            .build();
        return elasticsearchOperations.count(query, getEntityClass());
    }

    @Override
    public void deleteById(Long id) {
        Assert.notNull(id, "Cannot delete entity with id 'null'.");
        elasticsearchOperations.delete(elasticsearchOperations.getIndexName(),
            ElasticsearchOperations.INDEX_QUERY_TYPE,
            String.valueOf(id));
        refresh();
    }

    @Override
    public void delete(XmEntity entity) {
        Assert.notNull(entity, "Cannot delete 'null' entity.");
        deleteById(entity.getId());
        refresh();
    }

    @Override
    public void deleteAll(Iterable<? extends XmEntity> entities) {
        Assert.notNull(entities, "Cannot delete 'null' list.");
        for (XmEntity entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        DeleteQuery deleteQuery = new DeleteQuery();

        deleteQuery.setQuery(matchAllQuery());
        deleteQuery.setIndex(elasticsearchOperations.getIndexName());
        deleteQuery.setType(ElasticsearchOperations.INDEX_QUERY_TYPE);
        elasticsearchOperations.delete(deleteQuery, getEntityClass());
        refresh();
    }
}
