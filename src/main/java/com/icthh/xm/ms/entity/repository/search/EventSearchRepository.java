package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Event;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Event entity.
 */
public interface EventSearchRepository extends ElasticsearchRepository<Event, Long> {
}
