package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmFunction;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the XmFunction entity.
 */
public interface XmFunctionSearchRepository extends ElasticsearchRepository<XmFunction, Long> {
}
