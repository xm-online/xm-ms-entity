package com.icthh.xm.ms.entity.web.rest;

import static com.icthh.xm.ms.entity.web.rest.XmRestApiConstants.XM_HEADER_CONTENT_NAME;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.errors.ErrorConstants;
import com.icthh.xm.commons.errors.exception.BusinessException;
import com.icthh.xm.ms.entity.config.Constants;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.Profile;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.repository.kafka.ProfileEventProducer;
import com.icthh.xm.ms.entity.service.ProfileService;
import com.icthh.xm.ms.entity.service.api.XmEntityService;
import com.icthh.xm.ms.entity.util.XmHttpEntityUtils;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.PaginationUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

/**
 * REST controller for managing XmEntity.
 */
@RestController
@RequestMapping("/api")
public class XmEntityResource {

    private final Logger log = LoggerFactory.getLogger(XmEntityResource.class);

    private static final String ENTITY_NAME = "xmEntity";

    private final XmEntityService xmEntityService;

    private final ProfileService profileService;

    private final ProfileEventProducer profileEventProducer;

    public XmEntityResource(
                    @Qualifier("xmEntityServiceResolver") XmEntityService xmEntityService,
                    ProfileService profileService,
                    ProfileEventProducer profileEventProducer) {
        this.xmEntityService = xmEntityService;
        this.profileService = profileService;
        this.profileEventProducer = profileEventProducer;
    }

    /**
     * POST /xm-entities : Create a new xmEntity.
     * @param xmEntity the xmEntity to create
     * @return the ResponseEntity with status 201 (Created) and with body the new xmEntity, or with
     *         status 400 (Bad Request) if the xmEntity has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/xm-entities")
    @Timed
    public ResponseEntity<XmEntity> createXmEntity(@Valid @RequestBody XmEntity xmEntity) throws URISyntaxException {
        log.debug("REST request to save XmEntity : {}", xmEntity);
        if (xmEntity.getId() != null) {
            throw new BusinessException(ErrorConstants.ERR_BUSINESS_IDEXISTS,
                "A new XmEntity cannot already have an ID");
        }
        if (Constants.TENANT_TYPE_KEY.equals(xmEntity.getTypeKey()) && xmEntity.getName().trim().contains(" ")) {
            throw new BusinessException(ErrorConstants.ERR_VALIDATION,
                "Entity name can not contain whitespaces");
        }
        XmEntity result = xmEntityService.save(xmEntity);
        return ResponseEntity.created(new URI("/api/xm-entities/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, String.valueOf(result.getId())))
            .body(result);
    }

    /**
     * PUT /xm-entities : Updates an existing xmEntity.
     * @param xmEntity the xmEntity to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated xmEntity, or with
     *         status 400 (Bad Request) if the xmEntity is not valid, or with status 500 (Internal
     *         Server Error) if the xmEntity couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/xm-entities")
    @Timed
    public ResponseEntity<XmEntity> updateXmEntity(@Valid @RequestBody XmEntity xmEntity) throws URISyntaxException {
        log.debug("REST request to update XmEntity : {}", xmEntity);
        if (xmEntity.getId() == null) {
            return createXmEntity(xmEntity);
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
        log.debug("REST request to get a page of XmEntities");
        Page<XmEntity> page = xmEntityService.findAll(pageable, typeKey);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/xm-entities");
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
    public ResponseEntity<XmEntity> getXmEntity(@PathVariable String idOrKey,
        @RequestParam(required = false) List<String> embed) {
        log.debug("REST request to get XmEntity : {}, fields {}", idOrKey, embed);
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
    public ResponseEntity<Void> deleteXmEntity(@PathVariable Long id) {
        log.debug("REST request to delete XmEntity : {}", id);
        xmEntityService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/xm-entities?query=:query : search for the xmEntity corresponding to the query.
     *
     * @param query the query of the xmEntity search
     * @param pageable the pagination information
     * @return the result of the search
     */
    @GetMapping("/_search/xm-entities")
    @Timed
    public ResponseEntity<List<XmEntity>> searchXmEntities(@RequestParam String query, @ApiParam Pageable pageable) {
        log.debug("REST request to search for a page of XmEntities for query {}", query);
        Page<XmEntity> page = xmEntityService.search(query, pageable);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(query, page, "/api/_search/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * POST  /xm-entities/registration : register account.
     *
     * @param xmEntity the xmEntity to create
     * @return the ResponseEntity with status 200, or with status 400 (Bad Request)
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/xm-entities/registration")
    @Timed
    public ResponseEntity<Void> registration(@RequestBody XmEntity xmEntity) throws URISyntaxException {
        log.debug("REST request to register XmEntity : {}", xmEntity);
        xmEntityService.register(xmEntity);
        return ResponseEntity.ok().build();
    }

    /**
     * @see {@link com.icthh.xm.ms.entity.web.rest.XmFunctionResource#callFunction}
     * @deprecated use @POST /entity/api/xm-functions/{functionKey}
     */
    @Deprecated
    @PostMapping("/xm-entities/registration/activate/{code}")
    @Timed
    public ResponseEntity<Void> registration(@PathVariable String code) throws URISyntaxException {
        log.debug("REST request to activate account by code : {}", code);
        xmEntityService.activate(code);
        return ResponseEntity.ok().build();
    }

    @Deprecated
    @GetMapping("/xm-entities/profile")
    @Timed
    public ResponseEntity<XmEntity> getXmEntity() {
        XmEntity xmEntity = xmEntityService.profile();
        return RespContentUtil.wrapOrNotFound(Optional.ofNullable(xmEntity));
    }

    @Timed
    @PostMapping("/xm-entities/{idOrKey}/functions/{functionKey}")
    public ResponseEntity<XmFunction> executeFunction(@PathVariable String idOrKey,
        @PathVariable String functionKey,
        @RequestBody(required = false) Map<String, Object> context) {

        log.debug("REST request for XmEntity: {} to exec function: {} with context: {}", idOrKey, functionKey, context);

        XmFunction result = xmEntityService.executeFunction(IdOrKey.of(idOrKey), functionKey, context);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/xm-entities/{idOrKey}/links/targets")
    @Timed
    public List<Link> getLinkTargets(@PathVariable String idOrKey, @RequestParam(required = false) String typeKey) {
        log.debug("REST request to get Attachments for entity {} by typeKey {}", idOrKey, typeKey);
        return xmEntityService.getLinkTargets(IdOrKey.of(idOrKey), typeKey);
    }

    @GetMapping("/xm-entities/self/links/targets")
    @Timed
    public List<Link> getSelfLinkTargets(@RequestParam(required = false) String typeKey) {
        log.debug("REST request to get Attachments for self by typeKey {}", typeKey);
        return xmEntityService.getSelfLinkTargets(typeKey);
    }

    @PostMapping("/xm-entities/{idOrKey}/links/targets")
    @Timed
    public ResponseEntity<Link> saveLinkTarget(@PathVariable String idOrKey, @RequestPart("link") Link link,
        @RequestPart(value = "file", required = false) MultipartFile file) {
        log.debug("REST request to create target for entity {}", idOrKey);
        Link result = xmEntityService.saveLinkTarget(IdOrKey.of(idOrKey), link, file);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/xm-entities/self/links/targets")
    @Timed
    public ResponseEntity<Link> saveSelfLinkTarget(@RequestPart("link") Link link,
        @RequestPart(value = "file", required = false) MultipartFile file) {
        log.debug("REST request to create target for self");
        Link result = xmEntityService.saveSelfLinkTarget(link, file);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/xm-entities/{idOrKey}/links/targets/{targetId}")
    @Timed
    public ResponseEntity<Link> updateLinkTarget(@PathVariable String idOrKey, @PathVariable String targetId,
        @RequestPart("link") Link link, @RequestPart(value = "file", required = false) MultipartFile file) {
        log.debug("REST request to update target for entity {}", idOrKey);
        Link result = xmEntityService.updateLinkTarget(IdOrKey.of(idOrKey), targetId, link, file);
        return ResponseEntity.ok().body(result);
    }

    @PostMapping("/xm-entities/self/links/targets/{targetId}")
    @Timed
    public ResponseEntity<Link> updateSelfLinkTarget(@PathVariable String targetId,
        @RequestPart("link") Link link, @RequestPart(value = "file", required = false) MultipartFile file) {
        log.debug("REST request to update target for self");
        Link result = xmEntityService.updateSelfLinkTarget(targetId, link, file);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping("/xm-entities/{idOrKey}/links/targets/{targetId}")
    @Timed
    public ResponseEntity<Void> deleteLinkTarget(@PathVariable String idOrKey, @PathVariable String targetId) {
        log.debug("REST request to delete link target {} for entity {}", targetId, idOrKey);
        xmEntityService.deleteLinkTarget(IdOrKey.of(idOrKey), targetId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/xm-entities/self/links/targets/{targetId}")
    @Timed
    public ResponseEntity<Void> deleteSelfLinkTarget(@PathVariable String targetId) {
        log.debug("REST request to delete link target {} for self", targetId);
        xmEntityService.deleteSelfLinkTarget(targetId);
        return ResponseEntity.ok().build();
    }

    /**
     * @deprecated use {@link #updateXmEntityState(java.lang.String, java.lang.String, java.util.Map)} instead
     */
    @Deprecated
    @Timed
    @PutMapping("/xm-entities/{id}/states")
    public ResponseEntity<XmEntity> updateXmEntityState(@PathVariable String id,
        @RequestBody XmEntity xmEntity)
        throws URISyntaxException {
        log.debug("REST request to update XmEntity state : {}", xmEntity);
        xmEntityService.updateState(id, xmEntity);
        return ResponseEntity.ok().build();
    }

    @Timed
    @PutMapping("/xm-entities/{idOrKey}/states/{stateKey}")
    public ResponseEntity<XmEntity> updateXmEntityState(@PathVariable String idOrKey,
        @PathVariable String stateKey,
        @RequestBody(required = false) Map<String, Object> context) {
        log.debug("REST request to update XmEntity: {} state: {} with context: {}", idOrKey, stateKey, context);

        XmEntity xmEntity = xmEntityService.updateState(IdOrKey.of(idOrKey), stateKey, context);
        return ResponseEntity.ok().body(xmEntity);
    }

    @Timed
    @PostMapping("/xm-entities/{idOrKey}/avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable String idOrKey,
        @RequestParam("file") MultipartFile multipartFile)
        throws IOException {

        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(multipartFile);
        URI uri = xmEntityService.updateAvatar(IdOrKey.of(idOrKey), avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    @Timed
    @PutMapping(path = "/xm-entities/{idOrKey}/avatar", consumes = "image/*")
    public ResponseEntity<Void> updateAvatar(@PathVariable String idOrKey,
        @RequestHeader(value = XM_HEADER_CONTENT_NAME, required = false) String fileName,
        HttpServletRequest request) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(request, fileName);
        URI uri = xmEntityService.updateAvatar(IdOrKey.of(idOrKey), avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    @Timed
    @PostMapping("/xm-entities/self/avatar")
    public ResponseEntity<Void> updateSelfAvatar(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(multipartFile);
        URI uri = xmEntityService.updateSelfAvatar(avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    @Timed
    @PutMapping(path = "/xm-entities/self/avatar", consumes = "image/*")
    public ResponseEntity<Void> updateSelfAvatar(
        @RequestHeader(value = XM_HEADER_CONTENT_NAME, required = false) String fileName,
        HttpServletRequest request) throws IOException {
        HttpEntity<Resource> avatarEntity = XmHttpEntityUtils.buildAvatarHttpEntity(request, fileName);
        URI uri = xmEntityService.updateSelfAvatar(avatarEntity);
        return buildAvatarUpdateResponse(uri);
    }

    private static ResponseEntity<Void> buildAvatarUpdateResponse(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        if (uri != null) {
            headers.setLocation(uri);
        }
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @GetMapping("/_search-with-typekey/xm-entities")
    @Timed
    public ResponseEntity<List<XmEntity>> searchByTypeKeyAndQuery(@RequestParam String typeKey,
        @RequestParam(required = false) String query,
        @ApiParam Pageable pageable) {
        Page<XmEntity> page = xmEntityService.searchByQueryAndTypeKey(query, typeKey, pageable);
        HttpHeaders headers = PaginationUtil
            .generateSearchPaginationHttpHeaders(typeKey, page, "/api/_search-with-typekey/xm-entities");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
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
}
