package com.icthh.xm.ms.entity.repository.search;

import com.icthh.xm.ms.entity.domain.Attachment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring Data Elasticsearch repository for the Attachment entity.
 */
public interface AttachmentSearchRepository extends ElasticsearchRepository<Attachment, Long> {
}
