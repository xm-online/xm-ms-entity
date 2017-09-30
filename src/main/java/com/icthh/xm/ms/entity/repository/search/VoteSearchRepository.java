package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Vote;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Vote entity.
 */
public interface VoteSearchRepository extends ElasticsearchRepository<Vote, Long> {
}
