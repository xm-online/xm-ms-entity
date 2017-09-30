package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Link;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Link entity.
 */
public interface LinkSearchRepository extends ElasticsearchRepository<Link, Long> {
}
