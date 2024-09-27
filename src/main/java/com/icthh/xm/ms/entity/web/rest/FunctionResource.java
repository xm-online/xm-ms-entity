package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.web.rest.util.FunctionUtils;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.POST_URLENCODED;

/**
 * The {@link FunctionResource} class.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FunctionResource {

    private static final String ENTITY_NAME_FUNCTION_CONTEXT = "functionContext";

    private static final String ENTITY_NAME_FUNCTION_PATH = "/api/function-contexts/";

    private final FunctionService functionService;

    private FunctionResource self;

    public FunctionResource(@Qualifier("functionService") FunctionService functionService) {
        this.functionService = functionService;
    }

    @Autowired
    public void setSelf(@Lazy FunctionResource self) {
        this.self = self;
    }

    @Timed
    @GetMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.GET.CALL')")
    @PrivilegeDescription("Privilege to call get function")
    public ResponseEntity<Object> callGetFunction(@PathVariable("functionKey") String functionKey,
                                                           @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, "GET");
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PutMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.PUT.CALL')")
    @PrivilegeDescription("Privilege to call put function")
    public ResponseEntity<Object> callPutFunction(@PathVariable("functionKey") String functionKey,
                                                           @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, "PUT");
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PatchMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.PATCH.CALL')")
    @PrivilegeDescription("Privilege to call patch function")
    public ResponseEntity<Object> callPatchFunction(@PathVariable("functionKey") String functionKey,
                                                    @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, "PATCH");
        return ResponseEntity.ok().body(result.functionResult());
    }

    /**
     * POST  /functions/{functionKey} : Execute a function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 201 (Created) and with body the new FunctionContext,
     * or with status 400 (Bad Request) if the FunctionContext has already an ID
     */
    @Timed
    @PostMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.CALL')")
    @PrivilegeDescription("Privilege to execute a function by key (key in entity specification)")
    public ResponseEntity<Object> callPostFunction(@PathVariable("functionKey") String functionKey,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, "POST");
        return processCreatedResponse(result);
    }

    @Timed
    @PostMapping(value = "/functions/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE
    })
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.CALL')")
    @PrivilegeDescription("Privilege to execute a function by key (key in entity specification)")
    public ResponseEntity<Object> callPostFormFunction(@PathVariable("functionKey") String functionKey,
                                                       @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, POST_URLENCODED);
        return processCreatedResponse(result);
    }

    /**
     * DELETE  /functions/{functionKey} : Execute a function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 200 (Success) and with body the new FunctionContext,
     * or with status 400 (Bad Request) if the FunctionContext has already an ID
     */
    @Timed
    @DeleteMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.DELETE.CALL')")
    @PrivilegeDescription("Privilege to execute a function by key (key in entity specification)")
    public ResponseEntity<Object> callDeleteFunction(@PathVariable("functionKey") String functionKey,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, "DELETE");
        return ResponseEntity.ok().body(result.functionResult());
    }


    /**
     * POST  /functions/anonymous/{functionKey} : Execute an anonymous function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 201 (Created) and with body the new FunctionContext,
     * or with status 400 (Bad Request) if the FunctionContext has already an ID
     */
    @Timed
    @PostMapping("/functions/anonymous/{functionKey:.+}")
    public ResponseEntity<Object> callPostAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                            @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput, "POST");
        return processCreatedResponse(result);
    }

    @Timed
    @PostMapping(value = "/functions/anonymous/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput, POST_URLENCODED);
        return processCreatedResponse(result);
    }


    /**
     * GET  /functions/anonymous/{functionKey} : Execute an anonymous function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 200 (Success) and with body the new FunctionContext,
     * or with status 400 (Bad Request) if the FunctionContext has already an ID
     */
    @Timed
    @GetMapping("/functions/anonymous/{functionKey:.+}")
    public ResponseEntity<Object> callGetAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                            @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput, "GET");
        return ResponseEntity.ok().body(result.functionResult());
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/**")
    public ResponseEntity<Object> callGetFunction(HttpServletRequest request,
                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callGetFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/**")
    public ResponseEntity<Object> callPostFunction(HttpServletRequest request,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callPostFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormFunction(HttpServletRequest request,
                                                       @RequestParam(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callPostFormFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PutMapping("/functions/**")
    public ResponseEntity<Object> callPutFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callPutFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PatchMapping("/functions/**")
    public ResponseEntity<Object> callPatchFunction(HttpServletRequest request,
                                                    @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callPatchFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @DeleteMapping("/functions/**")
    public ResponseEntity<Object> callDeleteFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callDeleteFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/anonymous/**")
    public ResponseEntity<Object> callGetAnonymousFunction(HttpServletRequest request,
                                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callGetAnonymousFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/anonymous/**")
    public ResponseEntity<Object> callPostAnonymousFunction(HttpServletRequest request,
                                                            @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callPostAnonymousFunction(FunctionUtils.getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/anonymous/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormAnonymousFunction(HttpServletRequest request,
                                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callPostFormAnonymousFunction(FunctionUtils.getFunctionKey(request), functionInput);
    }

    private ResponseEntity<Object> processCreatedResponse(FunctionContext result) {
        return Optional.ofNullable(result.getId())
            .map(id -> URI.create(ENTITY_NAME_FUNCTION_PATH + id))
            .map(uri -> ResponseEntity.created(uri)
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME_FUNCTION_CONTEXT, String.valueOf(result.getId())))
                .body(result.functionResult()))
            .orElse(ResponseEntity
                .status(HttpStatus.CREATED.value())
                .body(result.functionResult()));
    }

}
