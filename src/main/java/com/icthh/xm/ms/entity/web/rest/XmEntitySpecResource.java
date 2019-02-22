package com.icthh.xm.ms.entity.web.rest;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.FunctionSpec;
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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * REST controller for managing XmEntitySpec.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class XmEntitySpecResource {

    private static final String ENTITY_NAME = "xmEntity";

    private XmEntitySpecService xmEntitySpecService;
    private XmEntityGeneratorService xmEntityGeneratorService;
    private DynamicPermissionCheckService dynamicPermissionCheckService;

    public XmEntitySpecResource(XmEntitySpecService xmEntitySpecService,
                                XmEntityGeneratorService xmEntityGeneratorService,
                                DynamicPermissionCheckService dynamicPermissionCheckService) {
        this.xmEntitySpecService = xmEntitySpecService;
        this.xmEntityGeneratorService = xmEntityGeneratorService;
        this.dynamicPermissionCheckService = dynamicPermissionCheckService;
    }

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
         * @param xmEntitySpecService specification supplier
         * @param filterFunction filter function
         * @return
         */
        private List<TypeSpec> getTypeSpec(XmEntitySpecService xmEntitySpecService,  Function<List<TypeSpec>, List<TypeSpec>> filterFunction) {
            return this.typeSpecSupplier.andThen(filterFunction).apply(xmEntitySpecService);
        }

    }

    public enum Filter2 {

        ALL((spec) -> true),
        APP(XmEntitySpecService.isApp()),
        NON_ABSTRACT(XmEntitySpecService.isNotAbstract());

        private Predicate<TypeSpec> typeSpecPredicate;

        Filter2(final Predicate<TypeSpec> typeSpecPredicate) {
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

        Filter2 filter2 = Filter2.NON_ABSTRACT;

        Stream<TypeSpec> res = filter2.getTypeSpec(xmEntitySpecService)
                                      .map(this::filterFunctions);
//            .collect(Coll);

        XmEntitySpecResource.Filter f = filter != null ? filter : Filter.ALL;
        BiFunction<TypeSpec, Set<String>, TypeSpec> mapper = xmEntitySpecService::filterTypeSpecByFunctionPermission;
        //get specs from typedEnum F, and then filter by mapper if feature is enabled
        return f.getTypeSpec(xmEntitySpecService, dynamicPermissionCheckService.dynamicFunctionListFilter(mapper));
    }

    private TypeSpec filterFunctions(TypeSpec spec) {

        return dynamicPermissionCheckService.filterTypeSpecByPermission2(spec,
                                                                         spec::getFunctions,
                                                                         spec::setFunctions,
                                                                         FunctionSpec::getKey);

//        return dynamicPermissionCheckService
//            .dynamicFunctionListFilter2(spec, xmEntitySpecService::filterTypeSpecByFunctionPermission);
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
        BiFunction<TypeSpec, Set<String>, TypeSpec> mapper = xmEntitySpecService::filterTypeSpecByFunctionPermission;

        Optional<TypeSpec> spec = xmEntitySpecService.getTypeSpecByKey(key).map(
            //filter spec content if feature enabled
            dynamicPermissionCheckService.dynamicFunctionFilter(mapper)
        );
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

}
