package com.icthh.xm.ms.entity.web.rest;


import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.search.dto.ElasticFetchSourceFilterDto;
import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.template.TemplateParamsHolder;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.web.rest.facade.XmEntityFacade;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Deprecated REST controller for search. Will be removed in next releases
 */
@Deprecated
@RestController
@RequestMapping("/api")
public class XmEntitySearchResource {

    private final XmEntityFacade xmEntityFacade;

    public XmEntitySearchResource(
        XmEntityFacade xmEntityFacade) {
        this.xmEntityFacade = xmEntityFacade;
    }

    /**
     * SEARCH  /_search/xm-entities?query=:query : search for the xmEntity corresponding to the query.
     *
     * @param query    the query of the xmEntity search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping(value = "/_search/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    
    @PreAuthorize("hasPermission({'query': #query}, 'XMENTITY.SEARCH.QUERY')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the query")
    public ResponseEntity<List<XmEntityDto>> searchXmEntities(
        @RequestParam String query,
        @ApiParam Pageable pageable) {
        Page<XmEntityDto> page = xmEntityFacade.search(query, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page,
                "/api/_search/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search/v2/xm-entities")
    
    @PreAuthorize("hasPermission({'query': #query}, 'XMENTITY.SEARCH.QUERY')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the query")
    public ResponseEntity<List<XmEntityDto>> searchXmEntitiesV2(
        @RequestParam String query,
        @ApiParam Pageable pageable,
        @ApiParam ElasticFetchSourceFilterDto fetchSourceFilterDto) {
        SearchDto searchDto = SearchDto.builder()
            .query(query)
            .pageable(pageable)
            .entityClass(XmEntity.class)
            .fetchSourceFilter(fetchSourceFilterDto)
            .build();
        Page<XmEntityDto> page = xmEntityFacade.searchV2(searchDto, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page,
                "/api/_search/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search-with-template/xm-entities")
    
    @PreAuthorize("hasPermission({'template': #template}, 'XMENTITY.SEARCH.TEMPLATE')")
    @PrivilegeDescription("Privilege to search for the xmEntity by query template")
    public ResponseEntity<List<XmEntityDto>> searchXmEntities(
        @RequestParam String template,
        @ApiParam TemplateParamsHolder templateParamsHolder,
        @ApiParam Pageable pageable) {
        Page<XmEntityDto> page = xmEntityFacade.search(template, templateParamsHolder, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchWithTemplatePaginationHttpHeaders(template, templateParamsHolder, page,
                "/api/_search-with-template/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search-with-typekey/xm-entities")
    
    @PreAuthorize("hasPermission({'typeKey': #typeKey, 'query': #query}, 'XMENTITY.SEARCH.TYPEKEY.QUERY')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the query(not required) and typeKey")
    public ResponseEntity<List<XmEntityDto>> searchByTypeKeyAndQuery(
        @RequestParam String typeKey,
        @RequestParam(required = false) String query,
        @ApiParam Pageable pageable) {
        Page<XmEntityDto> page = xmEntityFacade.searchByQueryAndTypeKey(query, typeKey, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchByTypeKeyPaginationHttpHeaders(typeKey, query, page,
                "/api/_search-with-typekey/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search-with-typekey-and-template/xm-entities")
    
    @PreAuthorize("hasPermission({'typeKey': #typeKey, 'template': #template}, 'XMENTITY.SEARCH.TYPEKEY.TEMPLATE')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the template(not required) and typeKey")
    public ResponseEntity<List<XmEntityDto>> searchByTypeKeyAndQuery(
        @RequestParam String typeKey,
        @RequestParam(required = false) String template,
        @ApiParam TemplateParamsHolder templateParamsHolder,
        @ApiParam Pageable pageable) {
        Page<XmEntityDto> page = xmEntityFacade.searchByQueryAndTypeKey(template, templateParamsHolder, typeKey, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchByTypeKeyWithTemplatePaginationHttpHeaders(typeKey, template, templateParamsHolder, page,
                "/api/_search-with-typekey-and-template/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    
    @GetMapping("/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}/search")
    @PreAuthorize("hasPermission({'query': #query, 'entityTypeKey': #entityTypeKey, 'linkTypeKey': #linkTypeKey}, 'XMENTITY.SEARCH.TO_LINK.QUERY')")
    @PrivilegeDescription("Privilege to search candidates for link with specified XmEntity")
    public ResponseEntity<List<XmEntityDto>> searchXmEntitiesToLink(
        @PathVariable String idOrKey,
        @PathVariable String entityTypeKey,
        @PathVariable String linkTypeKey,
        @RequestParam String query,
        @ApiParam Pageable pageable) {
        Page<XmEntityDto> page = xmEntityFacade
            .searchXmEntitiesToLink(IdOrKey.of(idOrKey), entityTypeKey, linkTypeKey, query, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page,
                String.format("/api/xm-entities/%s/%s/links/%s/search", entityTypeKey, idOrKey, linkTypeKey));
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
}
