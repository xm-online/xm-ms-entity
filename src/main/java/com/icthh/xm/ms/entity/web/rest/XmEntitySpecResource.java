package com.icthh.xm.ms.entity.web.rest;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.security.access.DynamicPermissionCheckService;
import com.icthh.xm.ms.entity.service.XmEntityGeneratorService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import com.icthh.xm.ms.entity.web.rest.util.RespContentUtil;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST controller for managing XmEntitySpec.
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class XmEntitySpecResource {

    private static final String ENTITY_NAME = "xmEntity";

    private final XmEntitySpecService xmEntitySpecService;
    private final XmEntityGeneratorService xmEntityGeneratorService;
    private final DynamicPermissionCheckService dynamicPermissionCheckService;

    public enum Filter {

        ALL((spec) -> true),
        APP(XmEntitySpecService.isApp()),
        NON_ABSTRACT(XmEntitySpecService.isNotAbstract());

        private Predicate<TypeSpec> typeSpecPredicate;

        Filter(final Predicate<TypeSpec> typeSpecPredicate) {
            this.typeSpecPredicate = typeSpecPredicate;
        }

        /**
         * getFilteredEntities
         */
        private Stream<TypeSpec> getTypeSpec(XmEntitySpecService xmEntitySpecService) {
            return xmEntitySpecService.findAllTypes()
                                      .stream()
                                      .filter(typeSpecPredicate);
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

        return Optional.ofNullable(filter).orElse(Filter.ALL)
                       .getTypeSpec(xmEntitySpecService)
                       .map(this::filterFunctions)
                       .collect(Collectors.toList());
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

        Optional<TypeSpec> spec = xmEntitySpecService.getTypeSpecByKey(key).map(this::filterFunctions);

        return RespContentUtil.wrapOrNotFound(spec);
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

    private TypeSpec filterFunctions(TypeSpec spec) {
        return dynamicPermissionCheckService.filterInnerListByPermission(spec,
                                                                         spec::getFunctions,
                                                                         spec::setFunctions,
                                                                         FunctionSpec::getKey);
    }

}
