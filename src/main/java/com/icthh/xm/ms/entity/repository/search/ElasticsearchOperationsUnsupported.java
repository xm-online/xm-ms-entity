package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.commons.search.ElasticsearchOperations;
import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.commons.search.mapper.extractor.ResultsExtractor;
import com.icthh.xm.commons.search.query.SearchQuery;
import com.icthh.xm.commons.search.query.dto.DeleteQuery;
import com.icthh.xm.commons.search.query.dto.GetQuery;
import com.icthh.xm.commons.search.query.dto.IndexQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ElasticsearchOperationsUnsupported implements ElasticsearchOperations {

    private final String SEARCH_UNSUPPORTED = "Search not supported in this version";

    @Override
    public String composeIndexName(String tenantCode) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public String getIndexName() {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> boolean createIndex(Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public boolean createIndex(String indexName) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public boolean createIndex(String indexName, Object settings) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public boolean putMapping(String indexName, String type, Object mappings) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> boolean putMapping(Class<T> clazz, Object mappings) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> T queryForObject(GetQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> List<T> queryForList(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> List<String> queryForIds(SearchQuery query) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> long count(SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> long count(SearchQuery query) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public String index(IndexQuery query) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> boolean indexExists(Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> boolean indexExists(String indexName) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public void bulkIndex(List<IndexQuery> queries) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public String delete(String indexName, String type, String id) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> String delete(Class<T> clazz, String id) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> void delete(DeleteQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public void delete(DeleteQuery query) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public boolean deleteIndex(String indexName) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> boolean deleteIndex(Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public void refresh(String indexName) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> void refresh(Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> void clearScroll(String scrollId) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> List<T> search(String query, Class<T> entityClass, String privilegeKey) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> search(Long scrollTimeInMillis,
                              String query,
                              Pageable pageable,
                              Class<T> entityClass,
                              String privilegeKey) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> searchByQueryAndTypeKey(String query,
                                               String typeKey,
                                               Pageable pageable,
                                               Class<T> entityClass,
                                               String privilegeKey) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }

    @Override
    public <T> Page<T> searchWithIdNotIn(String query,
                                         Set<Long> ids,
                                         String targetEntityTypeKey,
                                         Pageable pageable,
                                         Class<T> entityClass,
                                         String privilegeKey) {
        throw new UnsupportedOperationException(SEARCH_UNSUPPORTED);
    }
}
