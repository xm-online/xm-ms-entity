package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.TransactionPropagationService;
import com.icthh.xm.ms.entity.service.search.db.XmEntitySearchTextReindexService;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterParser;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateExecutor;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateType;
import com.icthh.xm.ms.entity.web.rest.facade.XmEntityDbSearchFacade;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search endpoints backed by the relational DB (no Elasticsearch).
 * Each GET normalizes its query string into a request object and then calls the POST method through the
 * Spring proxy, so {@code @PreAuthorize} evaluates exactly the data that the service executes.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class XmEntityDbSearchResource extends TransactionPropagationService<XmEntityDbSearchResource> {

    static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final Set<String> PAGE_PARAMS = Set.of("page", "size", "sort");

    private final XmEntityDbSearchFacade facade;
    private final XmEntitySearchTextReindexService reindexService;

    @GetMapping(value = "/_search-db/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<XmEntityDto>> searchGet(@RequestParam String typeKey,
                                                       @RequestParam(required = false) String query,
                                                       @RequestParam(required = false) Boolean includeSubTypes,
                                                       @RequestParam MultiValueMap<String, String> params,
                                                       @ParameterObject Pageable pageable) {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey(typeKey);
        request.setQuery(query);
        request.setIncludeSubTypes(includeSubTypes);
        request.setFilter(toFilterBody(params));
        request.setRawStringValues(true);
        return self.searchPost(request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #request.typeKey, 'query': #request.query, 'filter': #request.filter}, 'XMENTITY.SEARCH.DB.QUERY')")
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters")
    public ResponseEntity<List<XmEntityDto>> searchPost(@RequestBody XmEntityDbSearchRequest request,
                                                        @ParameterObject Pageable pageable) {
        return respond(request, pageable);
    }

    private ResponseEntity<List<XmEntityDto>> respond(XmEntityDbSearchRequest request, Pageable pageable) {
        Page<XmEntityDto> page = facade.search(request, pageable, null);
        return new ResponseEntity<>(page.getContent(), totalCount(page.getTotalElements()), HttpStatus.OK);
    }

    @GetMapping(value = "/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<XmEntityDto>> searchToLinkGet(@PathVariable String entityTypeKey,
                                                             @PathVariable String idOrKey,
                                                             @PathVariable String linkTypeKey,
                                                             @RequestParam(required = false) String query,
                                                             @RequestParam(required = false) Boolean includeSubTypes,
                                                             @RequestParam MultiValueMap<String, String> params,
                                                             @ParameterObject Pageable pageable) {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setQuery(query);
        request.setIncludeSubTypes(includeSubTypes);
        request.setFilter(toFilterBody(params));
        request.setRawStringValues(true);
        return self.searchToLinkPost(entityTypeKey, idOrKey, linkTypeKey, request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'entityTypeKey': #entityTypeKey, 'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #request.query, 'filter': #request.filter}, 'XMENTITY.SEARCH.DB.TO_LINK')")
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type")
    public ResponseEntity<List<XmEntityDto>> searchToLinkPost(@PathVariable String entityTypeKey,
                                                              @PathVariable String idOrKey,
                                                              @PathVariable String linkTypeKey,
                                                              @RequestBody XmEntityDbSearchRequest request,
                                                              @ParameterObject Pageable pageable) {
        return respondToLink(entityTypeKey, idOrKey, linkTypeKey, request, pageable);
    }

    @GetMapping(value = "/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LinkDto>> searchTargetsGet(@PathVariable String idOrKey,
                                                          @PathVariable String linkTypeKey,
                                                          @RequestParam(required = false) String typeKey,
                                                          @RequestParam(required = false) String query,
                                                          @RequestParam(required = false) Boolean includeSubTypes,
                                                          @RequestParam MultiValueMap<String, String> params,
                                                          @ParameterObject Pageable pageable) {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey(typeKey);
        request.setQuery(query);
        request.setIncludeSubTypes(includeSubTypes);
        request.setFilter(toFilterBody(params));
        request.setRawStringValues(true);
        return self.searchTargetsPost(idOrKey, linkTypeKey, request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #request.query, 'filter': #request.filter}, 'LINK.SEARCH.DB.TARGETS')")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB filtered by target fields")
    public ResponseEntity<List<LinkDto>> searchTargetsPost(@PathVariable String idOrKey,
                                                           @PathVariable String linkTypeKey,
                                                           @RequestBody XmEntityDbSearchRequest request,
                                                           @ParameterObject Pageable pageable) {
        return respondTargets(idOrKey, linkTypeKey, request, pageable);
    }

    private ResponseEntity<List<XmEntityDto>> respondToLink(String entityTypeKey, String idOrKey, String linkTypeKey,
                                                            XmEntityDbSearchRequest request, Pageable pageable) {
        Page<XmEntityDto> page = facade.searchToLink(IdOrKey.of(idOrKey), entityTypeKey, linkTypeKey, request, pageable, null);
        return new ResponseEntity<>(page.getContent(), totalCount(page.getTotalElements()), HttpStatus.OK);
    }

    private ResponseEntity<List<LinkDto>> respondTargets(String idOrKey, String linkTypeKey,
                                                         XmEntityDbSearchRequest request, Pageable pageable) {
        Page<LinkDto> page = facade.searchTargets(IdOrKey.of(idOrKey), linkTypeKey, request, pageable, null);
        return new ResponseEntity<>(page.getContent(), totalCount(page.getTotalElements()), HttpStatus.OK);
    }

    @GetMapping(value = "/_search-db/xm-entities/template/{templateKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<?>> searchByTemplateGet(@PathVariable String templateKey,
                                                       @RequestParam MultiValueMap<String, String> params,
                                                       @ParameterObject Pageable pageable) {
        Map<String, Object> templateParams = new LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (!PAGE_PARAMS.contains(k) && v != null && !v.isEmpty()) {
                templateParams.put(k, v.get(0));
            }
        });
        return self.searchByTemplatePost(templateKey, templateParams, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/template/{templateKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'templateKey': #templateKey, 'params': #params}, 'XMENTITY.SEARCH.DB.TEMPLATE')")
    @PrivilegeDescription("Privilege to search xm entities in DB by a JPQL template")
    public ResponseEntity<List<?>> searchByTemplatePost(@PathVariable String templateKey,
                                                        @RequestBody(required = false) Map<String, Object> params,
                                                        @ParameterObject Pageable pageable) {
        return respondTemplate(templateKey, params == null ? Map.of() : params, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/reindex", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #typeKey}, 'XMENTITY.SEARCH.DB.REINDEX')")
    @PrivilegeDescription("Privilege to rebuild search_text of xm entities for DB full text search")
    public ResponseEntity<Map<String, Long>> reindex(@RequestParam(required = false) String typeKey) {
        return ResponseEntity.ok(Map.of("processed", reindexService.reindex(typeKey)));
    }

    private ResponseEntity<List<?>> respondTemplate(String templateKey, Map<String, Object> params, Pageable pageable) {
        if (facade.templateType(templateKey) == JpqlTemplateType.RAW) {
            JpqlTemplateExecutor.RawResult result = facade.searchByRawTemplate(templateKey, params, pageable);
            HttpHeaders headers = result.total() == null ? new HttpHeaders() : totalCount(result.total());
            return new ResponseEntity<>(result.rows(), headers, HttpStatus.OK);
        }
        Page<XmEntityDto> page = facade.searchByEntityTemplate(templateKey, params, pageable);
        return new ResponseEntity<>(page.getContent(), totalCount(page.getTotalElements()), HttpStatus.OK);
    }

    private static HttpHeaders totalCount(long total) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(TOTAL_COUNT_HEADER, Long.toString(total));
        return headers;
    }

    /** Keeps GET filter params as raw strings; the service infers their types. */
    static Map<String, Object> toFilterBody(MultiValueMap<String, String> params) {
        Map<String, Object> filter = new LinkedHashMap<>();
        params.forEach((key, values) -> {
            if (!FilterParser.RESERVED_PARAMS.contains(key) && values != null && !values.isEmpty()) {
                filter.put(key, values.get(0));
            }
        });
        return filter;
    }
}
