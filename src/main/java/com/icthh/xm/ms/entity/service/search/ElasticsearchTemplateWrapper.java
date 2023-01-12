package com.icthh.xm.ms.entity.service.search;


import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.GetResultMapper;
import org.springframework.data.elasticsearch.core.MultiGetResultMapper;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.query.AliasQuery;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.GetQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.MoreLikeThisQuery;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.util.CloseableIterator;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ElasticsearchTemplateWrapper implements ElasticsearchOperations {

    private final ElasticsearchTemplate elasticsearchTemplate;

    @Override
    public ElasticsearchConverter getElasticsearchConverter() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Client getClient() {
        return elasticsearchTemplate.getClient();
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        return elasticsearchTemplate.createIndex(clazz);
    }

    @Override
    public boolean createIndex(String indexName) {
        return elasticsearchTemplate.createIndex(indexName);
    }

    @Override
    public boolean createIndex(String indexName, Object settings) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz, Object settings) {
        return elasticsearchTemplate.createIndex(clazz, settings);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
        return elasticsearchTemplate.putMapping(clazz);
    }

    @Override
    public boolean putMapping(String indexName, String type, Object mappings) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz, Object mappings) {
        return elasticsearchTemplate.putMapping(clazz, mappings);
    }

    @Override
    public <T> Map getMapping(Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Map getMapping(String indexName, String type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Map getSetting(String indexName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Map getSetting(Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T queryForObject(StringQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> queryForPage(CriteriaQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> CloseableIterator<T> stream(CriteriaQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> CloseableIterator<T> stream(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> List<T> queryForList(CriteriaQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> long count(CriteriaQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> long count(CriteriaQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> long count(SearchQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> LinkedList<T> multiGet(SearchQuery searchQuery, Class<T> clazz, MultiGetResultMapper multiGetResultMapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String index(IndexQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public UpdateResponse update(UpdateQuery updateQuery) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void bulkIndex(List<IndexQuery> queries) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void bulkUpdate(List<UpdateQuery> queries) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String delete(String indexName, String type, String id) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> void delete(CriteriaQuery criteriaQuery, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> void delete(DeleteQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void delete(DeleteQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz) {
        return elasticsearchTemplate.deleteIndex(clazz);
    }

    @Override
    public boolean deleteIndex(String indexName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> boolean indexExists(Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean indexExists(String indexName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean typeExists(String index, String type) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void refresh(String indexName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> void refresh(Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz, SearchResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz, SearchResultMapper mapper) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> void clearScroll(String scrollId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> Page<T> moreLikeThis(MoreLikeThisQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Boolean addAlias(AliasQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Boolean removeAlias(AliasQuery query) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public List<AliasMetaData> queryForAlias(String indexName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ElasticsearchPersistentEntity getPersistentEntityFor(Class clazz) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
