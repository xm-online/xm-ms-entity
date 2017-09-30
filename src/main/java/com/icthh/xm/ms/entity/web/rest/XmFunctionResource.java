package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.domain.XmFunction;
import com.icthh.xm.ms.entity.service.XmFunctionService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing XmFunction.
 */
@RestController
@RequestMapping("/api")
public class XmFunctionResource {

    private final Logger log = LoggerFactory.getLogger(XmFunctionResource.class);

    private static final String ENTITY_NAME = "xmFunction";

    private final XmFunctionService xmFunctionService;

    public XmFunctionResource(@Qualifier("xmFunctionServiceResolver") XmFunctionService xmFunctionService) {
        this.xmFunctionService = xmFunctionService;
    }

    /**
     * POST  /xm-functions : Create a new xmFunction.
     *
     * @param xmFunction the xmFunction to create
     * @return the ResponseEntity with status 201 (Created) and with body the new xmFunction, or with status 400 (Bad Request) if the xmFunction has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/xm-functions")
    @Timed
    public ResponseEntity<XmFunction> createXmFunction(@Valid @RequestBody XmFunction xmFunction) throws URISyntaxException {
        log.debug("REST request to save XmFunction : {}", xmFunction);
        if (xmFunction.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new xmFunction cannot already have an ID")).body(null);
        }
        XmFunction result = xmFunctionService.save(xmFunction);
        return ResponseEntity.created(new URI("/api/xm-functions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * POST  /xm-functions/call/{functionKey} : Execute a function by key (key in entity specification).
     *
     * @param functionKey the function key to execute
     * @param context     function input data context
     * @return the ResponseEntity with status 201 (Created) and with body the new xmFunction, or with status 400 (Bad Request) if the xmFunction has already an ID
     */
    @Timed
    @PostMapping("/xm-functions/call/{functionKey:.+}")
    public ResponseEntity<XmFunction> callFunction(@PathVariable("functionKey") String functionKey,
                                                   @RequestBody Map<String, Object> context) {
        log.debug("REST request call function by key '{}' for any entity instance",
                  functionKey);
        XmFunction result = xmFunctionService.execute(functionKey, context);
        return ResponseEntity.created(URI.create("/api/xm-functions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /xm-functions : Updates an existing xmFunction.
     *
     * @param xmFunction the xmFunction to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated xmFunction,
     * or with status 400 (Bad Request) if the xmFunction is not valid,
     * or with status 500 (Internal Server Error) if the xmFunction couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/xm-functions")
    @Timed
    public ResponseEntity<XmFunction> updateXmFunction(@Valid @RequestBody XmFunction xmFunction) throws URISyntaxException {
        log.debug("REST request to update XmFunction : {}", xmFunction);
        if (xmFunction.getId() == null) {
            return createXmFunction(xmFunction);
        }
        XmFunction result = xmFunctionService.save(xmFunction);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, xmFunction.getId().toString()))
            .body(result);
    }

    /**
     * GET  /xm-functions : get all the xmFunctions.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of xmFunctions in body
     */
    @GetMapping("/xm-functions")
    @Timed
    public List<XmFunction> getAllXmFunctions() {
        log.debug("REST request to get all XmFunctions");
        return xmFunctionService.findAll();
    }

    /**
     * GET  /xm-functions/:id : get the "id" xmFunction.
     *
     * @param id the id of the xmFunction to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the xmFunction, or with status 404 (Not Found)
     */
    @GetMapping("/xm-functions/{id}")
    @Timed
    public ResponseEntity<XmFunction> getXmFunction(@PathVariable Long id) {
        log.debug("REST request to get XmFunction : {}", id);
        XmFunction xmFunction = xmFunctionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(xmFunction));
    }

    /**
     * DELETE  /xm-functions/:id : delete the "id" xmFunction.
     *
     * @param id the id of the xmFunction to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/xm-functions/{id}")
    @Timed
    public ResponseEntity<Void> deleteXmFunction(@PathVariable Long id) {
        log.debug("REST request to delete XmFunction : {}", id);
        xmFunctionService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/xm-functions?query=:query : search for the xmFunction corresponding
     * to the query.
     *
     * @param query the query of the xmFunction search
     * @return the result of the search
     */
    @GetMapping("/_search/xm-functions")
    @Timed
    public List<XmFunction> searchXmFunctions(@RequestParam String query) {
        log.debug("REST request to search XmFunctions for query {}", query);
        return xmFunctionService.search(query);
    }

}
