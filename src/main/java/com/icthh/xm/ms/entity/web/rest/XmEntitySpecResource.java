package com.icthh.xm.ms.entity.web.rest;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntityGeneratorService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * REST controller for managing XmEntitySpec.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class XmEntitySpecResource {

    private static final String ENTITY_NAME = "xmEntity";

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityGeneratorService xmEntityGeneratorService;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;

    public enum Filter {

        ALL(XmEntitySpecService::findAllTypes),
        APP(XmEntitySpecService::findAllAppTypes),
        NON_ABSTRACT(XmEntitySpecService::findAllNonAbstractTypes);

        private Function<XmEntitySpecService, List<TypeSpec>> typeSpecSupplier;

        Filter(Function<XmEntitySpecService, List<TypeSpec>> typeSpecSupplier){
            this.typeSpecSupplier = typeSpecSupplier;
        }

        /**
         * getFilteredEntities
         * @param xmEntitySpecService
         * @param filterFunction
         * @return
         */
        private List<TypeSpec> getTypeSpec(XmEntitySpecService xmEntitySpecService,  Function<List<TypeSpec>, List<TypeSpec>> filterFunction) {
            return this.typeSpecSupplier.andThen(filterFunction).apply(xmEntitySpecService);
        }

    }

    /**
     * GET  /xm-entity-specs : get the typeSpecs.
     *
     * @param filter the type specs ()
     * @return the ResponseEntity with status 200 (OK) and the list of xmEntitySpecs in body
     */
    @GetMapping("/xm-entity-specs")
    @Timed
    @PostFilter("hasPermission({'returnObject': filterObject, 'log': false}, 'XMENTITY_SPEC.GET')")
    public List<TypeSpec> getTypeSpecs(@ApiParam XmEntitySpecResource.Filter filter) {
        log.debug("REST request to get a list of TypeSpec");
        XmEntitySpecResource.Filter f = filter != null ? filter : Filter.ALL;
        Function<TypeSpec, TypeSpec> mapper = spec -> spec;
        return f.getTypeSpec(xmEntitySpecService, dynamicPermissionCheckService.dynamicFunctionFilter(mapper));
    }

    /**
     * GET  /xm-entity-specs/:key : get the "key" typeSpec.
     *
     * @param key the key of the typeSpec to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the typeSpec, or with status 404 (Not Found)
     */
    @GetMapping("/xm-entity-specs/{key}")
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'XMENTITY_SPEC.GET_LIST.ITEM')")
    public ResponseEntity<TypeSpec> getTypeSpec(@PathVariable String key) {
        log.debug("REST request to get TypeSpec : {}", key);
        return RespContentUtil.wrapOrNotFound(xmEntitySpecService.getTypeSpecByKey(key));
    }

    /**
     * POST  /xm-entity-specs/generate-xm-entity : Generate a new xmEntity.
     *
     * @return the ResponseEntity with status 201 (Created) and with body the new xmEntity
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/xm-entity-specs/generate-xm-entity")
    @Timed
    @PreAuthorize("hasPermission({'rootTypeKey': #rootTypeKey}, 'XMENTITY_SPEC.GENERATE')")
    public ResponseEntity<XmEntity> generateXmEntity(@ApiParam String rootTypeKey) throws URISyntaxException {
        log.debug("REST request to generate XmEntity");
        XmEntity result = xmEntityGeneratorService.generateXmEntity(rootTypeKey != null ? rootTypeKey : "");
        return ResponseEntity.created(new URI("/api/xm-entities/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * POST  /xm-entity-specs/ : Generate a new xmEntity.
     *
     * @return the ResponseEntity with status 201 (Created) and with body the new xmEntity
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping(value = "/xm-entity-specs", consumes = {TEXT_PLAIN_VALUE})
    @Timed
    @PreAuthorize("hasPermission({'xmEntitySpec': #xmEntitySpec}, 'XMENTITY_SPEC.UPDATE')")
    public ResponseEntity<XmEntity> updateXmEntitySpec(@RequestBody String xmEntitySpec) {
        xmEntitySpecService.updateXmEntitySpec(xmEntitySpec);
        return ResponseEntity.ok().build();
    }

}
