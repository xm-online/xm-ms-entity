package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.FunctionContext;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the FunctionContext entity.
 */
public interface FunctionContextSearchRepository extends ElasticsearchRepository<FunctionContext, Long> {
}
