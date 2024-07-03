package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.service.search.ElasticsearchTemplateWrapper;
import com.icthh.xm.ms.entity.service.search.builder.NativeSearchQueryBuilder;
import com.icthh.xm.ms.entity.service.search.builder.QueryBuilder;
import com.icthh.xm.ms.entity.service.search.query.SearchQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.DeleteQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.GetQuery;
import com.icthh.xm.ms.entity.service.search.query.dto.IndexQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class XmEntitySearchRepositoryImpl implements XmEntitySearchRepository {

    private final TenantContextHolder tenantContextHolder;
    private final ElasticsearchTemplateWrapper elasticsearchTemplateWrapper;

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
        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantContextHolder.getTenantKey());
        elasticsearchTemplateWrapper.refresh(indexName);
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
//            .withQuery(QueryBuilders.matchAllQuery()) TODO-IMPL
            .withPageable(PageRequest.of(0, itemCount, sort)).build();
        return elasticsearchTemplateWrapper.queryForPage(query, getEntityClass());
    }

    @Override
    public Page<XmEntity> findAll(Pageable pageable) {
        SearchQuery query = new NativeSearchQueryBuilder()
//            .withQuery(matchAllQuery()) TODO-IMPL
            .withIndices(elasticsearchTemplateWrapper.getIndexName())
//            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE) TODO-IMPL: Removed in 8.14v
            .withPageable(pageable)
            .build();
        return elasticsearchTemplateWrapper.queryForPage(query, getEntityClass());
    }

    @Override
    public <S extends XmEntity> S save(S entity) {
        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantContextHolder.getTenantKey());
        elasticsearchTemplateWrapper.index(createIndexQuery(indexName, entity));
        return entity;
    }

    @Override
    public <S extends XmEntity> Iterable<S> saveAll(Iterable<S> entities) {
        String indexName = ElasticsearchTemplateWrapper.composeIndexName(tenantContextHolder.getTenantKey());

        List<IndexQuery> queries = new ArrayList<>();
        for (S s : entities) {
            queries.add(createIndexQuery(indexName, s));
        }

        elasticsearchTemplateWrapper.bulkIndex(queries);
        elasticsearchTemplateWrapper.refresh(indexName);
        return entities;
    }

    private <S extends XmEntity> IndexQuery createIndexQuery(String indexName, S entity) {
        IndexQuery query = new IndexQuery();
    // TODO-IMPL
//        query.setObject(entity);
//        query.setId(String.valueOf(entity.getId()));
//        query.setType(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE);
//        query.setIndexName(indexName);

//        query.setVersion(extractVersionFromBean(entity)); // TODO
//        query.setParentId(extractParentIdFromBean(entity)); // TODO
        return query;
    }

    @Override
    public Optional<XmEntity> findById(Long id) {
        GetQuery query = new GetQuery();
        query.setId(String.valueOf(id));
        return Optional.ofNullable(elasticsearchTemplateWrapper.queryForObject(query, getEntityClass()));
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
//            .withQuery(matchAllQuery()) TODO-IMPL
            .withIndices(elasticsearchTemplateWrapper.getIndexName())
//            .withTypes(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE)
            .build();
        return elasticsearchTemplateWrapper.count(query, getEntityClass());
    }

    @Override
    public void deleteById(Long id) {
        Assert.notNull(id, "Cannot delete entity with id 'null'.");
        elasticsearchTemplateWrapper.delete(elasticsearchTemplateWrapper.getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE,
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
    // TODO-IMPL
//        deleteQuery.setQuery(matchAllQuery());
//        deleteQuery.setIndex(elasticsearchTemplateWrapper.getIndexName());
//        deleteQuery.setType(ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE);
        elasticsearchTemplateWrapper.delete(deleteQuery, getEntityClass());
        refresh();
    }
}
