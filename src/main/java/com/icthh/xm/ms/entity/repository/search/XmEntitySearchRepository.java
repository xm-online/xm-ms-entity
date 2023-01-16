package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.XmEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the XmEntity entity.
 *
 * @Deprecation - repository methods should be rewritten to avoid usage of org.springframework.data.elasticsearch
 */
@Deprecated
public interface XmEntitySearchRepository extends ElasticsearchRepository<XmEntity, Long> {
}
