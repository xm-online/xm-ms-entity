package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Calendar;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Calendar entity.
 */
public interface CalendarSearchRepository extends ElasticsearchRepository<Calendar, Long> {
}
