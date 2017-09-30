package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Rating;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Rating entity.
 */
public interface RatingSearchRepository extends ElasticsearchRepository<Rating, Long> {
}
