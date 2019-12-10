package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the XmEntity entity.
 */
public interface XmEntitySearchRepository extends ElasticsearchRepository<XmEntity, Long> {
}
