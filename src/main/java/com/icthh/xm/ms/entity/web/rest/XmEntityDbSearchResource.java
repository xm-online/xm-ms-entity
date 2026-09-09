package com.icthh.xm.ms.entity.web.rest;

import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import com.icthh.xm.ms.entity.service.search.db.filter.FilterParser;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplate;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateExecutor;
import com.icthh.xm.ms.entity.service.search.db.template.JpqlTemplateType;
import com.icthh.xm.ms.entity.web.rest.facade.XmEntityDbSearchFacade;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

/** Search endpoints backed by the relational DB (no Elasticsearch). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class XmEntityDbSearchResource {

    static final String SEARCH_URL = "/api/_search-db/xm-entities";
    private static final Set<String> PAGE_PARAMS = Set.of("page", "size", "sort");

    private final XmEntityDbSearchFacade facade;

    @GetMapping(value = "/_search-db/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #typeKey, 'query': #query, 'filter': #params}, 'XMENTITY.SEARCH.DB.QUERY')")
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters (GET)")
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
        return respond(request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'typeKey': #request.typeKey, 'query': #request.query, 'filter': #request.filter}, 'XMENTITY.SEARCH.DB.QUERY')")
    @PrivilegeDescription("Privilege to search xm entities in DB by typeKey, full text query and filters (POST)")
    public ResponseEntity<List<XmEntityDto>> searchPost(@RequestBody XmEntityDbSearchRequest request,
                                                        @ParameterObject Pageable pageable) {
        return respond(request, pageable);
    }

    private ResponseEntity<List<XmEntityDto>> respond(XmEntityDbSearchRequest request, Pageable pageable) {
        Page<XmEntityDto> page = facade.search(request, pageable, null);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams(request, pageable), page, SEARCH_URL);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'entityTypeKey': #entityTypeKey, 'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #query, 'filter': #params}, 'XMENTITY.SEARCH.DB.TO_LINK')")
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type (GET)")
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
        return respondToLink(entityTypeKey, idOrKey, linkTypeKey, request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'entityTypeKey': #entityTypeKey, 'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #request.query, 'filter': #request.filter}, 'XMENTITY.SEARCH.DB.TO_LINK')")
    @PrivilegeDescription("Privilege to search link candidates in DB for an xm entity and link type (POST)")
    public ResponseEntity<List<XmEntityDto>> searchToLinkPost(@PathVariable String entityTypeKey,
                                                              @PathVariable String idOrKey,
                                                              @PathVariable String linkTypeKey,
                                                              @RequestBody XmEntityDbSearchRequest request,
                                                              @ParameterObject Pageable pageable) {
        return respondToLink(entityTypeKey, idOrKey, linkTypeKey, request, pageable);
    }

    @GetMapping(value = "/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #query, 'filter': #params}, 'LINK.SEARCH.DB.TARGETS')")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB filtered by target fields (GET)")
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
        return respondTargets(idOrKey, linkTypeKey, request, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/{idOrKey}/targets/{linkTypeKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'linkTypeKey': #linkTypeKey, 'query': #request.query, 'filter': #request.filter}, 'LINK.SEARCH.DB.TARGETS')")
    @PrivilegeDescription("Privilege to search links of a source xm entity in DB filtered by target fields (POST)")
    public ResponseEntity<List<LinkDto>> searchTargetsPost(@PathVariable String idOrKey,
                                                           @PathVariable String linkTypeKey,
                                                           @RequestBody XmEntityDbSearchRequest request,
                                                           @ParameterObject Pageable pageable) {
        return respondTargets(idOrKey, linkTypeKey, request, pageable);
    }

    private ResponseEntity<List<XmEntityDto>> respondToLink(String entityTypeKey, String idOrKey, String linkTypeKey,
                                                            XmEntityDbSearchRequest request, Pageable pageable) {
        Page<XmEntityDto> page = facade.searchToLink(IdOrKey.of(idOrKey), entityTypeKey, linkTypeKey, request, pageable, null);
        String url = String.format("/api/_search-db/xm-entities/%s/%s/links/%s", entityTypeKey, idOrKey, linkTypeKey);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams(request, pageable), page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    private ResponseEntity<List<LinkDto>> respondTargets(String idOrKey, String linkTypeKey,
                                                         XmEntityDbSearchRequest request, Pageable pageable) {
        Page<LinkDto> page = facade.searchTargets(IdOrKey.of(idOrKey), linkTypeKey, request, pageable, null);
        String url = String.format("/api/_search-db/xm-entities/%s/targets/%s", idOrKey, linkTypeKey);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams(request, pageable), page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/_search-db/xm-entities/template/{templateKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'templateKey': #templateKey, 'params': #params}, 'XMENTITY.SEARCH.DB.TEMPLATE')")
    @PrivilegeDescription("Privilege to search xm entities in DB by a JPQL template (GET)")
    public ResponseEntity<List<?>> searchByTemplateGet(@PathVariable String templateKey,
                                                       @RequestParam MultiValueMap<String, String> params,
                                                       @ParameterObject Pageable pageable) {
        Map<String, Object> templateParams = new LinkedHashMap<>();
        params.forEach((k, v) -> {
            if (!PAGE_PARAMS.contains(k) && v != null && !v.isEmpty()) {
                templateParams.put(k, v.get(0));
            }
        });
        return respondTemplate(templateKey, templateParams, pageable);
    }

    @PostMapping(value = "/_search-db/xm-entities/template/{templateKey}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission({'templateKey': #templateKey, 'params': #params}, 'XMENTITY.SEARCH.DB.TEMPLATE')")
    @PrivilegeDescription("Privilege to search xm entities in DB by a JPQL template (POST)")
    public ResponseEntity<List<?>> searchByTemplatePost(@PathVariable String templateKey,
                                                        @RequestBody(required = false) Map<String, Object> params,
                                                        @ParameterObject Pageable pageable) {
        return respondTemplate(templateKey, params == null ? Map.of() : params, pageable);
    }

    private ResponseEntity<List<?>> respondTemplate(String templateKey, Map<String, Object> params, Pageable pageable) {
        JpqlTemplate template = facade.template(templateKey);
        String url = "/api/_search-db/xm-entities/template/" + templateKey;
        Map<String, Object> linkParams = new LinkedHashMap<>(params);
        if (pageable.getSort().isSorted()) {
            linkParams.put("sort", pageable.getSort().stream()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase()).toList());
        }
        if (template.getType() == JpqlTemplateType.RAW) {
            JpqlTemplateExecutor.RawResult result = facade.searchByRawTemplate(template, params, pageable);
            if (result.total() == null) {
                return ResponseEntity.ok(result.rows());
            }
            Page<Map<String, Object>> page = new PageImpl<>(result.rows(), pageable, result.total());
            HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams, page, url);
            return new ResponseEntity<>(result.rows(), headers, HttpStatus.OK);
        }
        Page<XmEntityDto> page = facade.searchByEntityTemplate(template, params, pageable, null);
        HttpHeaders headers = PaginationUtil.generateDbSearchPaginationHttpHeaders(linkParams, page, url);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
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

    static Map<String, Object> linkParams(XmEntityDbSearchRequest request, Pageable pageable) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("typeKey", request.getTypeKey());
        params.put("query", request.getQuery());
        params.put("includeSubTypes", request.getIncludeSubTypes());
        if (request.getFilter() != null) {
            params.putAll(request.getFilter());
        }
        if (pageable.getSort().isSorted()) {
            params.put("sort", pageable.getSort().stream()
                .map(o -> o.getProperty() + "," + o.getDirection().name().toLowerCase()).toList());
        }
        return params;
    }
}
