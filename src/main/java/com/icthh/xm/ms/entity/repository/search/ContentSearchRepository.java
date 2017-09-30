package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Content;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Content entity.
 */
public interface ContentSearchRepository extends ElasticsearchRepository<Content, Long> {
}
