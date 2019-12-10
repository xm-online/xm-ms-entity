package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionContextService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;

/**
 * REST controller for managing FunctionContext.
 */
@RestController
@RequestMapping("/api")
public class FunctionContextResource {

    private static final String ENTITY_NAME = "functionContext";

    private final FunctionContextService functionContextService;
    private final FunctionContextResource functionContextResource;

    public FunctionContextResource(
                    FunctionContextService functionContextService,
                    @Lazy FunctionContextResource functionContextResource) {
        this.functionContextService = functionContextService;
        this.functionContextResource = functionContextResource;
    }

    /**
     * POST  /function-contexts : Create a new functionContext.
     *
     * @param functionContext the functionContext to create
     * @return the ResponseEntity with status 201 (Created) and with body the new functionContext, or with status 400 (Bad Request) if the functionContext has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/function-contexts")
    @Timed
    @PreAuthorize("hasPermission({'functionContext': #functionContext}, 'FUNCTION_CONTEXT.CREATE')")
    public ResponseEntity<FunctionContext> createFunctionContext(@Valid @RequestBody FunctionContext functionContext) throws URISyntaxException {
        if (functionContext.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new functionContext cannot already have an ID")).body(null);
        }
        FunctionContext result = functionContextService.save(functionContext);
        return ResponseEntity.created(new URI("/api/function-contexts/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /function-contexts : Updates an existing functionContext.
     *
     * @param functionContext the functionContext to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated functionContext,
     * or with status 400 (Bad Request) if the functionContext is not valid,
     * or with status 500 (Internal Server Error) if the functionContext couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/function-contexts")
    @Timed
    @PreAuthorize("hasPermission({'id': #functionContext.id, 'newFunctionContext': #functionContext}, 'functionContext', 'FUNCTION_CONTEXT.UPDATE')")
    public ResponseEntity<FunctionContext> updateFunctionContext(@Valid @RequestBody FunctionContext functionContext) throws URISyntaxException {
        if (functionContext.getId() == null) {
            //in order to call method with permissions check
            return this.functionContextResource.createFunctionContext(functionContext);
        }
        FunctionContext result = functionContextService.save(functionContext);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, functionContext.getId().toString()))
            .body(result);
    }

    /**
     * GET  /function-contexts : get all the functionContexts.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of functionContexts in body
     */
    @GetMapping("/function-contexts")
    @Timed
    public List<FunctionContext> getAllFunctionContexts() {
        return functionContextService.findAll(null);
    }

    /**
     * GET  /function-contexts/:id : get the "id" functionContext.
     *
     * @param id the id of the functionContext to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the functionContext, or with status 404 (Not Found)
     */
    @GetMapping("/function-contexts/{id}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'FUNCTION_CONTEXT.GET_LIST.ITEM')")
    public ResponseEntity<FunctionContext> getFunctionContext(@PathVariable Long id) {
        FunctionContext functionContext = functionContextService.findOne(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(functionContext));
    }

    /**
     * DELETE  /function-contexts/:id : delete the "id" functionContext.
     *
     * @param id the id of the functionContext to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/function-contexts/{id}")
    @Timed
    @PreAuthorize("hasPermission({'id': #id}, 'functionContext', 'FUNCTION_CONTEXT.DELETE')")
    public ResponseEntity<Void> deleteFunctionContext(@PathVariable Long id) {
        functionContextService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
