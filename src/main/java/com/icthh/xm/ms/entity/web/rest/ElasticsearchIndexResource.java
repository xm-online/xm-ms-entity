package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.service.ElasticsearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing Elasticsearch index.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ElasticsearchIndexResource {

    private final ElasticsearchIndexService elasticsearchIndexService;

    /**
     * POST  /elasticsearch/index -> Reindex all Elasticsearch documents
     */
    @PostMapping("/elasticsearch/index")
    @Timed
    @PreAuthorize("hasPermission(null, 'ELASTICSEARCH.INDEX')")
    public ResponseEntity<Void> reindexAll() {
        elasticsearchIndexService.reindexAllAsync();
        return ResponseEntity.accepted().build();
    }
}
