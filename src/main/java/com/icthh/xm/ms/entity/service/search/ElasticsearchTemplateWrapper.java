package com.icthh.xm.ms.entity.service.search;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.ElasticsearchException;
import org.springframework.data.elasticsearch.core.DefaultResultMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.GetResultMapper;
import org.springframework.data.elasticsearch.core.MultiGetResultMapper;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.ResultsMapper;
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@RequiredArgsConstructor
public class ElasticsearchTemplateWrapper implements ElasticsearchOperations {

    public static final String INDEX_QUERY_TYPE = "xmentity";

    private final TenantContextHolder tenantContextHolder;
    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ObjectMapper objectMapper;
    private final ResultsMapper resultsMapper;

    public static String composeIndexName(String tenantCode) {
        return tenantCode.toLowerCase() + "_" + INDEX_QUERY_TYPE;
    }

    public String getIndexName() {
        String tenantKey = tenantContextHolder.getTenantKey();
        return composeIndexName(tenantKey);
    }

    @Override
    public ElasticsearchConverter getElasticsearchConverter() {
        return elasticsearchTemplate.getElasticsearchConverter();
    }

    @Override
    public Client getClient() {
        return elasticsearchTemplate.getClient();
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        return createIndex(getIndexName());
    }

    @Override
    public boolean createIndex(String indexName) {
        return elasticsearchTemplate.createIndex(indexName);
    }

    @Override
    public boolean createIndex(String indexName, Object settings) {
        return elasticsearchTemplate.createIndex(indexName, settings);
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz, Object settings) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
        return elasticsearchTemplate.putMapping(getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, getDefaultMapping());
    }

    @Override
    public boolean putMapping(String indexName, String type, Object mappings) {
        return elasticsearchTemplate.putMapping(indexName, type, mappings);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz, Object mappings) {
        return elasticsearchTemplate.putMapping(getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, mappings);
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
        return queryForObject(query, clazz, resultsMapper);
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz, GetResultMapper mapper) {
        GetResponse response = elasticsearchTemplate.getClient()
            .prepareGet(getIndexName(), ElasticsearchTemplateWrapper.INDEX_QUERY_TYPE, query.getId()).execute()
            .actionGet();

        T entity = mapper.mapResult(response, clazz);
        return entity;
    }

    @Override
    public <T> T queryForObject(CriteriaQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForObject(query, clazz);
    }

    @Override
    public <T> T queryForObject(StringQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForObject(query, clazz);
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForPage(query, clazz);
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
        return elasticsearchTemplate.queryForPage(query, clazz, mapper);
    }

    @Override
    public <T> Page<T> queryForPage(CriteriaQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForPage(query, clazz);
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForPage(query, clazz);
    }

    @Override
    public <T> Page<T> queryForPage(StringQuery query, Class<T> clazz, SearchResultMapper mapper) {
        return elasticsearchTemplate.queryForPage(query, clazz, mapper);
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
        return elasticsearchTemplate.queryForList(query, clazz);
    }

    @Override
    public <T> List<T> queryForList(StringQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForList(query, clazz);
    }

    @Override
    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        return elasticsearchTemplate.queryForList(query, clazz);
    }

    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
        return elasticsearchTemplate.queryForIds(query);
    }

    @Override
    public <T> long count(CriteriaQuery query, Class<T> clazz) {
        return elasticsearchTemplate.count(query, clazz);
    }

    @Override
    public <T> long count(CriteriaQuery query) {
        return elasticsearchTemplate.count(query);
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
        return elasticsearchTemplate.count(query, clazz);
    }

    @Override
    public <T> long count(SearchQuery query) {
       return elasticsearchTemplate.count(query);
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
        return prepareIndex(query).execute().actionGet().getId();
    }

    private IndexRequestBuilder prepareIndex(IndexQuery query) {
        String indexName = query.getIndexName();
        String type = query.getType();

        IndexRequestBuilder indexRequestBuilder = null;
        try {
            Client client = getClient();
            if (query.getObject() != null) {
                String id = query.getId();
                // If we have a query id and a document id, do not ask ES to generate one.
                if (id != null) {
                    indexRequestBuilder = client.prepareIndex(indexName, type, id);
                } else {
                    indexRequestBuilder = client.prepareIndex(indexName, type);
                }
                indexRequestBuilder.setSource(objectMapper.writeValueAsString(query.getObject()),
                    Requests.INDEX_CONTENT_TYPE);
            } else if (query.getSource() != null) {
                indexRequestBuilder = client.prepareIndex(indexName, type, query.getId()).setSource(query.getSource(),
                    Requests.INDEX_CONTENT_TYPE);
            } else {
                throw new ElasticsearchException(
                    "object or source is null, failed to index the document [id: " + query.getId() + "]");
            }

            if (query.getParentId() != null) {
                indexRequestBuilder.setParent(query.getParentId());
            }

            return indexRequestBuilder;
        } catch (IOException e) {
            throw new ElasticsearchException("failed to index the document [id: " + query.getId() + "]", e);
        }
    }

    @Override
    public UpdateResponse update(UpdateQuery updateQuery) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void bulkIndex(List<IndexQuery> queries) {
        elasticsearchTemplate.bulkIndex(queries);
    }

    @Override
    public void bulkUpdate(List<UpdateQuery> queries) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String delete(String indexName, String type, String id) {
        return elasticsearchTemplate.delete(indexName, type, id);
    }

    @Override
    public <T> void delete(CriteriaQuery criteriaQuery, Class<T> clazz) {
        elasticsearchTemplate.delete(criteriaQuery, clazz);
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
        return elasticsearchTemplate.delete(clazz, id);
    }

    @Override
    public <T> void delete(DeleteQuery query, Class<T> clazz) {
        elasticsearchTemplate.delete(query, clazz);
    }

    @Override
    public void delete(DeleteQuery query) {
        elasticsearchTemplate.delete(query);
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz) {
        return deleteIndex(getIndexName());
    }

    @Override
    public boolean deleteIndex(String indexName) {
        return elasticsearchTemplate.deleteIndex(indexName);
    }

    @Override
    public <T> boolean indexExists(Class<T> clazz) {
        return indexExists(getIndexName());
    }

    @Override
    public boolean indexExists(String indexName) {
        return elasticsearchTemplate.indexExists(indexName);
    }

    @Override
    public boolean typeExists(String index, String type) {
        return elasticsearchTemplate.typeExists(index, type);
    }

    @Override
    public void refresh(String indexName) {
        elasticsearchTemplate.refresh(indexName);
    }

    @Override
    public <T> void refresh(Class<T> clazz) {
        refresh(getIndexName());
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz) {
        return elasticsearchTemplate.startScroll(scrollTimeInMillis, query, clazz);
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz, SearchResultMapper mapper) {
        return elasticsearchTemplate.startScroll(scrollTimeInMillis, query, clazz, mapper);
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz) {
        return elasticsearchTemplate.startScroll(scrollTimeInMillis, criteriaQuery, clazz);
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, CriteriaQuery criteriaQuery, Class<T> clazz, SearchResultMapper mapper) {
        return elasticsearchTemplate.startScroll(scrollTimeInMillis, criteriaQuery, clazz, mapper);
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
        return elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, clazz);
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz, SearchResultMapper mapper) {
        return elasticsearchTemplate.continueScroll(scrollId, scrollTimeInMillis, clazz, mapper);
    }

    @Override
    public <T> void clearScroll(String scrollId) {
        elasticsearchTemplate.clearScroll(scrollId);
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

    private String getDefaultMapping() {
        String location = "/config/elastic/default-mapping.json";
        try {
            return IOUtils.toString(new ClassPathResource(location).getInputStream(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
