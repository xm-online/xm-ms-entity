package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.ms.entity.web.rest.FunctionResource.getFunctionKey;
import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.search.dto.ElasticFetchSourceFilterDto;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.domain.template.TemplateParamsHolder;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.TenantService;
import com.icthh.xm.ms.entity.service.XmEntityService;
import com.icthh.xm.ms.entity.service.dto.LinkSourceDto;
import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for managing XmEntity.
 */
@RestController
@RequestMapping("/api")
public class XmEntityResource {

    private static final String ENTITY_NAME = "xmEntity";
    private static final String TENANT_USED_CODE = "xm.xmEntity.tenant.error.alreadyUsed";

    private final XmEntityService xmEntityService;

    private final ProfileService profileService;

    private final ProfileEventProducer profileEventProducer;

    private final FunctionService functionService;

    private final TenantService tenantService;

    private final XmEntityResource xmEntityResource;

    public XmEntityResource(
        XmEntityService xmEntityService,
        ProfileService profileService,
        ProfileEventProducer profileEventProducer,
        FunctionService functionService,
        TenantService tenantService,
        @Lazy XmEntityResource xmEntityResource) {
        this.xmEntityService = xmEntityService;
        this.profileService = profileService;
        this.profileEventProducer = profileEventProducer;
        this.functionService = functionService;
        this.tenantService = tenantService;
        this.xmEntityResource = xmEntityResource;
    }

    /**
     * POST /xm-entities : Create a new xmEntity.
     *
     * @param xmEntity the xmEntity to create
     * @return the ResponseEntity with status 201 (Created) and with body the new xmEntity, or with
     * status 400 (Bad Request) if the xmEntity has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/xm-entities")
    @Timed
    @PreAuthorize("hasPermission({'xmEntity': #xmEntity}, 'XMENTITY.CREATE')")
    @PrivilegeDescription("Privilege to create a tenant")
    public ResponseEntity<XmEntity> createXmEntity(@Valid @RequestBody XmEntity xmEntity) throws URISyntaxException {
        if (xmEntity.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                                        "A new XmEntity cannot already have an ID");
        }
        if (Constants.TENANT_TYPE_KEY.equals(xmEntity.getTypeKey())) {
            tenantService.validateTenantKey(xmEntity.getName());
            if (xmEntityService.existsByTypeKeyIgnoreCase(xmEntity.getTypeKey(), xmEntity.getName())) {
                throw new BusinessException(TENANT_USED_CODE, "Tenant already exists");
            }
        }
        XmEntity result = xmEntityService.save(xmEntity);
        return ResponseEntity.created(new URI("/api/xm-entities/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, String.valueOf(result.getId())))
            .body(result);
    }

    /**
     * PUT /xm-entities : Updates an existing xmEntity.
     *
     * @param xmEntity the xmEntity to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated xmEntity, or with
     * status 400 (Bad Request) if the xmEntity is not valid, or with status 500 (Internal
     * Server Error) if the xmEntity couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/xm-entities")
    @Timed
    @PreAuthorize("hasPermission({'id': #xmEntity.id, 'newXmEntity': #xmEntity}, 'xmEntity', 'XMENTITY.UPDATE')")
    @PrivilegeDescription("Privilege to updates an existing xmEntity")
    public ResponseEntity<XmEntity> updateXmEntity(@Valid @RequestBody XmEntity xmEntity) throws URISyntaxException {
        if (xmEntity.getId() == null) {
            //in order to call method with permissions check
            return this.xmEntityResource.createXmEntity(xmEntity);
        }
        XmEntity result = xmEntityService.save(xmEntity);
        if (Constants.ACCOUNT_TYPE_KEY.equals(xmEntity.getTypeKey())) {
            produceEvent(result, Constants.UPDATE_ACCOUNT);
        }
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, xmEntity.getId().toString()))
            .body(result);
    }

    /**
     * GET  /xm-entities : get all the xmEntities.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of xmEntities in body
     */
    @GetMapping("/xm-entities")
    @Timed
    public ResponseEntity<List<XmEntity>> getAllXmEntities(@ApiParam Pageable pageable,
                                                           @RequestParam(required = false) String typeKey) {
        Page<XmEntity> page = xmEntityService.findAll(pageable, typeKey, null);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/xm-entities-by-ids")
    @Timed
    public ResponseEntity<List<XmEntity>> getXmEntitiesByIds(@ApiParam Pageable pageable,
                                                             @RequestParam Set<Long> ids,
                                                             @RequestParam(required = false) Set<String> embed) {
        Page<XmEntity> page = xmEntityService.findByIds(pageable, ids, embed, null);
        HttpHeaders headers = PaginationUtil.generateByIdsPaginationHttpHeaders(ids, embed, page, "/api/xm-entities-by-ids");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET  /xm-entities/:id : get the "id" xmEntity.
     *
     * @param idOrKey the id of the xmEntity to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the xmEntity, or with status 404 (Not Found)
     */
    @GetMapping("/xm-entities/{idOrKey}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'XMENTITY.GET_LIST.ITEM')")
    @PrivilegeDescription("Privilege to get the xmEntity by id or key")
    public ResponseEntity<XmEntity> getXmEntity(@PathVariable String idOrKey,
                                                @RequestParam(required = false) List<String> embed) {
        XmEntity xmEntity;
        if (isEmpty(embed)) {
            xmEntity = xmEntityService.findOne(IdOrKey.of(idOrKey));
        } else {
            xmEntity = xmEntityService.findOne(IdOrKey.of(idOrKey), embed);
        }

        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(xmEntity));
    }

    /**
     * DELETE  /xm-entities/:id : delete the "id" xmEntity.
     *
     * @param id the id of the xmEntity to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/xm-entities/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'xmEntity', 'XMENTITY.DELETE')")
    @PrivilegeDescription("Privilege to delete the xmEntity by id")
    public ResponseEntity<Void> deleteXmEntity(@PathVariable Long id) {
        xmEntityService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/xm-entities?query=:query : search for the xmEntity corresponding to the query.
     *
     * @param query    the query of the xmEntity search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping(value = "/_search/xm-entities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PreAuthorize("hasPermission({'query': #query}, 'XMENTITY.SEARCH.QUERY')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the query")
    public ResponseEntity<List<XmEntity>> searchXmEntities(
            @RequestParam String query,
            @ApiParam Pageable pageable) {
        Page<XmEntity> page = xmEntityService.search(query, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page,
                "/api/_search/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search/v2/xm-entities")
    @Timed
    @PreAuthorize("hasPermission({'query': #query}, 'XMENTITY.SEARCH.QUERY')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the query")
    public ResponseEntity<List<XmEntity>> searchXmEntitiesV2(
        @RequestParam String query,
        @ApiParam Pageable pageable,
        @ApiParam ElasticFetchSourceFilterDto fetchSourceFilterDto) {
        SearchDto searchDto = SearchDto.builder()
            .query(query)
            .pageable(pageable)
            .entityClass(XmEntity.class)
            .fetchSourceFilter(fetchSourceFilterDto)
            .build();
        Page<XmEntity> page = xmEntityService.searchV2(searchDto, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page,
                "/api/_search/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search-with-template/xm-entities")
    @Timed
    @PreAuthorize("hasPermission({'template': #template}, 'XMENTITY.SEARCH.TEMPLATE')")
    @PrivilegeDescription("Privilege to search for the xmEntity by query template")
    public ResponseEntity<List<XmEntity>> searchXmEntities(
        @RequestParam String template,
        @ApiParam TemplateParamsHolder templateParamsHolder,
        @ApiParam Pageable pageable) {
        Page<XmEntity> page = xmEntityService.search(template, templateParamsHolder, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchWithTemplatePaginationHttpHeaders(template, templateParamsHolder, page,
                "/api/_search-with-template/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search-with-typekey/xm-entities")
    @Timed
    @PreAuthorize("hasPermission({'typeKey': #typeKey, 'query': #query}, 'XMENTITY.SEARCH.TYPEKEY.QUERY')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the query(not required) and typeKey")
    public ResponseEntity<List<XmEntity>> searchByTypeKeyAndQuery(
            @RequestParam String typeKey,
            @RequestParam(required = false) String query,
            @ApiParam Pageable pageable) {
        Page<XmEntity> page = xmEntityService.searchByQueryAndTypeKey(query, typeKey, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchByTypeKeyPaginationHttpHeaders(typeKey, query, page,
                "/api/_search-with-typekey/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_search-with-typekey-and-template/xm-entities")
    @Timed
    @PreAuthorize("hasPermission({'typeKey': #typeKey, 'template': #template}, 'XMENTITY.SEARCH.TYPEKEY.TEMPLATE')")
    @PrivilegeDescription("Privilege to search for the xmEntity corresponding to the template(not required) and typeKey")
    public ResponseEntity<List<XmEntity>> searchByTypeKeyAndQuery(
        @RequestParam String typeKey,
        @RequestParam(required = false) String template,
        @ApiParam TemplateParamsHolder templateParamsHolder,
        @ApiParam Pageable pageable) {
        Page<XmEntity> page = xmEntityService.searchByQueryAndTypeKey(template, templateParamsHolder, typeKey, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchByTypeKeyWithTemplatePaginationHttpHeaders(typeKey, template, templateParamsHolder, page,
                "/api/_search-with-typekey-and-template/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @Deprecated
    @GetMapping("/xm-entities/profile")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'XMENTITY.GET_PROFILE')")
    @PrivilegeDescription("Privilege to get xmEntity of current user profile")
    public ResponseEntity<XmEntity> getXmEntity() {
        XmEntity xmEntity = xmEntityService.profile();
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(xmEntity));
    }

    /**
     * Call XM entity function via GET method. Used in xm-entity related flows and widgets
     * Authorization is done in private {@executeFunction}
     * @param idOrKey entity ID or Key
     * @param functionKey function key
     * @param functionInput - optional input
     * @return any object
     */
    @Timed
    @GetMapping("/xm-entities/{idOrKey}/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'id': #functionKey, 'context': #context}, "
        + "'xmEntity.function', 'XMENTITY.FUNCTION.EXECUTE')")
    @PrivilegeDescription("Privilege to execute get function on xmEntity")
    public ResponseEntity<Object> executeGetFunction(@PathVariable String idOrKey,
                                                     @PathVariable String functionKey,
                                                     @RequestParam(required = false) Map<String, Object> functionInput) {
        return executeFunction(idOrKey, functionKey, functionInput);
    }

    /**
     * Call XM entity function via POST method. Used in xm-entity related actions
     * Authorization is done in private {@executeFunction}
     * @param idOrKey entity ID or Key
     * @param functionKey function key
     * @param functionInput - optional input
     * @return any object
     */
    @Timed
    @PostMapping("/xm-entities/{idOrKey}/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'idOrKey': #idOrKey, 'id': #functionKey, 'context': #context}, "
        + "'xmEntity.function', 'XMENTITY.FUNCTION.EXECUTE')")
    @PrivilegeDescription("Privilege to execute post function on xmEntity")
    public ResponseEntity<Object> executePostFunction(@PathVariable String idOrKey,
                                                           @PathVariable String functionKey,
                                                           @RequestBody(required = false) Map<String, Object> functionInput) {
        return executeFunction(idOrKey, functionKey, functionInput);
    }

    @Timed
    @GetMapping("/xm-entities/{idOrKey}/functions/**")
    public ResponseEntity<Object> executeGetFunction(@PathVariable String idOrKey,
                                                     HttpServletRequest request,
                                                     @RequestParam(required = false) Map<String, Object> functionInput) {
        return xmEntityResource.executeGetFunction(idOrKey, getFunctionKey(request), functionInput);
    }

    @Timed
    @PostMapping("/xm-entities/{idOrKey}/functions/**")
    public ResponseEntity<Object> executeFunction(@PathVariable String idOrKey,
                                                  HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        return xmEntityResource.executePostFunction(idOrKey, getFunctionKey(request), functionInput);
    }

    @GetMapping("/xm-entities/{idOrKey}/links/targets")
    @Timed
    @PostFilter("hasPermission({'returnObject': filterObject, 'log': false}, 'XMENTITY.LINK.TARGET.GET_LIST')")
    @PrivilegeDescription("Privilege to get all target links with entities referenced by indicated entity")
    public List<Link> getLinkTargets(@PathVariable String idOrKey, @RequestParam(required = false) String typeKey) {
        return xmEntityService.getLinkTargets(IdOrKey.of(idOrKey), typeKey);
    }

    @Timed
    @GetMapping("/xm-entities/{entityTypeKey}/{idOrKey}/links/{linkTypeKey}/search")
    @PreAuthorize("hasPermission({'query': #query, 'entityTypeKey': #entityTypeKey, 'linkTypeKey': #linkTypeKey}, 'XMENTITY.SEARCH.TO_LINK.QUERY')")
    @PrivilegeDescription("Privilege to search candidates for link with specified XmEntity")
    public ResponseEntity<List<XmEntity>> searchXmEntitiesToLink(
        @PathVariable String idOrKey,
        @PathVariable String entityTypeKey,
        @PathVariable String linkTypeKey,
        @RequestParam String query,
        @ApiParam Pageable pageable) {
        Page<XmEntity> page = xmEntityService
            .searchXmEntitiesToLink(IdOrKey.of(idOrKey), entityTypeKey, linkTypeKey, query, pageable, null);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page,
                String.format("/api/xm-entities/%s/%s/links/%s/search", entityTypeKey, idOrKey, linkTypeKey));
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @PostMapping("/xm-entities/{idOrKey}/links/targets")
    @Timed
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'link':#link, 'file':#file}, 'XMENTITY.LINK.TARGET.CREATE')")
    @PrivilegeDescription("Privilege to add target link to indicated xmEntity")
    public ResponseEntity<Link> saveLinkTarget(@PathVariable String idOrKey,
                                               @RequestPart("link") Link link,
                                               @RequestPart(value = "file", required = false) MultipartFile file) {
        Link result = xmEntityService.saveLinkTarget(IdOrKey.of(idOrKey), link, file);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/xm-entities/{idOrKey}/links/targets/{targetId}")
    @Timed
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'id':#targetId, 'link':#link, 'file':#file}, "
        + "'xmEntity.link.target', 'XMENTITY.LINK.TARGET.UPDATE')")
    @PrivilegeDescription("Privilege to update an existing target link in the entity")
    public ResponseEntity<Link> updateLinkTarget(@PathVariable String idOrKey,
                                                 @PathVariable String targetId,
                                                 @RequestPart("link") Link link,
                                                 @RequestPart(value = "file", required = false) MultipartFile file) {
        Link result = xmEntityService.updateLinkTarget(IdOrKey.of(idOrKey), targetId, link, file);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/xm-entities/{idOrKey}/links/targets/{targetId}")
    @Timed
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'id':#targetId}, 'xmEntity.link.target', 'XMENTITY.LINK.TARGET.DELETE')")
    @PrivilegeDescription("Privilege to delete target link from")
    public ResponseEntity<Void> deleteLinkTarget(@PathVariable String idOrKey, @PathVariable String targetId) {
        xmEntityService.deleteLinkTarget(IdOrKey.of(idOrKey), targetId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/xm-entities/{idOrKey}/links/sources")
    @Timed
    @PostFilter("hasPermission({'returnObject': filterObject, 'log': false}, 'XMENTITY.LINK.SOURCE.GET_LIST')")
    @PrivilegeDescription("Privilege to get all source links with entities referenced by indicated entity")
    public List<Link> getLinkSources(@PathVariable String idOrKey, @RequestParam(required = false) String typeKey) {
        return xmEntityService.getLinkSources(IdOrKey.of(idOrKey), typeKey);
    }

    @GetMapping("/v2/xm-entities/{idOrKey}/links/sources")
    @Timed
    public ResponseEntity<List<LinkSourceDto>> getLinkSourcesInverted(@PathVariable String idOrKey,
                                                                      @RequestParam(required = false) Set<String>
                                                                          typeKeys,
                                                                      @ApiParam Pageable pageable) {

        Page<LinkSourceDto> page = xmEntityService.getLinkSourcesInverted(pageable,
                                                                          IdOrKey.of(idOrKey),
                                                                          typeKeys,
                                                                          null);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page,
                                                                           "/v2/api/xm-entities/" + idOrKey
                                                                           + "/links/sources");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);

    }

    /**
     * @deprecated use {@link #updateXmEntityState(java.lang.String, java.lang.String, java.util.Map)} instead
     */
    @Deprecated
    @Timed
    @PutMapping("/xm-entities/{id}/states")
    @PreAuthorize("hasPermission({'id':#id, 'xmEntity':#xmEntity}, 'xmEntity.state', 'XMENTITY.STATE.UPDATE')")
    @PrivilegeDescription("Privilege to update xmEntity state")
    public ResponseEntity<XmEntity> updateXmEntityState(@PathVariable String id,
                                                        @RequestBody XmEntity xmEntity)
        throws URISyntaxException {
        xmEntityService.updateState(id, xmEntity);
        return ResponseEntity.ok().build();
    }

    @Timed
    @PutMapping("/xm-entities/{idOrKey}/states/{stateKey}")
    @PreAuthorize("hasPermission({'id': #idOrKey, 'stateKey':#stateKey, 'context':#context}, 'xmEntity', 'XMENTITY.STATE')")
    @PrivilegeDescription("Privilege to update xmEntity state")
    public ResponseEntity<XmEntity> updateXmEntityState(@PathVariable String idOrKey,
                                                        @PathVariable String stateKey,
                                                        @RequestBody(required = false) Map<String, Object> context) {
        XmEntity xmEntity = xmEntityService.updateState(IdOrKey.of(idOrKey), stateKey, context);
        return ResponseEntity.ok().body(xmEntity);
    }

    /**
     * Multipart update avatar.
     *
     * @param idOrKey       id or key of XmEntity
     * @param multipartFile multipart file with avatar
     * @return HTTP response
     * @throws IOException when IO error
     */
    @Timed
    @PostMapping("/xm-entities/{idOrKey}/avatar")
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'multipartFile':#multipartFile}, 'XMENTITY.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable String idOrKey,
                                             @RequestParam("file") MultipartFile multipartFile)
        throws IOException {

        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(multipartFile);
        URI uri = xmEntityService.updateAvatar(IdOrKey.of(idOrKey), avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    // TODO remove this method after deal with hasPermission check for self
    @Timed
    @PostMapping("/xm-entities/self/avatar")
    @PreAuthorize("hasPermission({'multipartFile': #multipartFile}, 'XMENTITY.SELF.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar of current user")
    public ResponseEntity<Void> updateSelfAvatar(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(multipartFile);
        URI uri = xmEntityService.updateAvatar(IdOrKey.SELF, avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    /**
     * Update avatar with simple PUT HTTP request.
     *
     * @param idOrKey  id or key of XmEntity
     * @param fileName avatar file name
     * @param request  HTTP servlet request
     * @return HTTP response
     * @throws IOException when IO error
     */
    @Timed
    @PutMapping(path = "/xm-entities/{idOrKey}/avatar", consumes = "image/*")
    @PreAuthorize("hasPermission({'idOrKey':#idOrKey, 'fileName':#fileName, 'request':#request}, 'XMENTITY.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable String idOrKey,
                                             @RequestHeader(value = XM_HEADER_CONTENT_NAME, required = false)
                                                 String fileName,
                                             HttpServletRequest request) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(request, fileName);
        URI uri = xmEntityService.updateAvatar(IdOrKey.of(idOrKey), avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    // TODO remove this method after deal with hasPermission check for self
    @Timed
    @PutMapping(path = "/xm-entities/self/avatar", consumes = "image/*")
    @PreAuthorize("hasPermission({'fileName':#fileName, 'request':#request}, 'XMENTITY.SELF.AVATAR.UPDATE')")
    @PrivilegeDescription("Privilege to update avatar of current user")
    public ResponseEntity<Void> updateSelfAvatar(@RequestHeader(value = XM_HEADER_CONTENT_NAME, required = false)
                                                     String fileName,
                                                 HttpServletRequest request) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(request, fileName);
        URI uri = xmEntityService.updateAvatar(IdOrKey.SELF, avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    private static ResponseEntity<Void> buildAvatarUpdateResponse(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        if (uri != null) {
            headers.setLocation(uri);
        }
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    /**
     * GET  /xm-entities/export : export all xmEntities by typeKey in specific file format.
     * @param fileFormat the file format
     * @param typeKey the entity type key
     * @return file with entities
     */
    @Timed
    @GetMapping("/xm-entities/export")
    @PreAuthorize("hasPermission({'fileFormat':#fileFormat, 'typeKey':#typeKey}, 'XMENTITY.EXPORT.FILE')")
    @PrivilegeDescription("Privilege to export all xmEntities by typeKey in specific file format")
    public ResponseEntity<byte[]> exportEntities(
                    @ApiParam(name = "fileFormat", value = "Specify file format to download(csv, xlsx, etc)")
                    @RequestParam String fileFormat,
                    @RequestParam String typeKey) throws IOException {
        byte[] media = xmEntityService.exportEntities(fileFormat, typeKey);

        return ResponseEntity.ok().contentLength(media.length)
                        .headers(HeaderUtil.createDownloadEntityHeaders(typeKey, fileFormat)).body(media);
    }

    private void produceEvent(XmEntity entity, String eventType) {
        if (entity == null) {
            return;
        }
        Profile profile = profileService.getByXmEntityId(entity.getId());
        if (profile == null) {
            return;
        }
        String content = this.profileEventProducer.createEventJson(profile, eventType);
        this.profileEventProducer.send(content);
    }

    private ResponseEntity<Object> executeFunction(String idOrKey,
                                                  String functionKey,
                                                  Map<String, Object> functionInput) {
        Map<String, Object> fContext = functionInput != null ? functionInput : Maps.newHashMap();
        FunctionContext result = functionService.execute(functionKey, IdOrKey.of(idOrKey), fContext);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();

        if (result.isBinaryData()) {
            response.header(HttpHeaders.CONTENT_TYPE, result.getBinaryDataType());
        }

        return response.body(result.functionResult());
    }

}
