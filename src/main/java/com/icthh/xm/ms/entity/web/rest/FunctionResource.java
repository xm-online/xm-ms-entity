package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.ms.entity.config.Constants.MVC_FUNC_RESULT;
import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.POST_URLENCODED;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

/**
 * The {@link FunctionResource} class.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FunctionResource {

    private static final String ENTITY_NAME_FUNCTION_CONTEXT = "functionContext";

    private static final String ENTITY_NAME_FUNCTION_PATH = "/api/function-contexts/";

    private static final String UPLOAD = "/upload";

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

    /**
     * POST  /functions/mvc/{functionKey} : Execute a mvc function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return ModelAndView object
     */
    @Timed
    @PostMapping("/functions/mvc/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.MVC.CALL')")
    @PrivilegeDescription("Privilege to execute a mvc function by key (key in entity specification)")
    public ModelAndView callMvcFunction(@PathVariable("functionKey") String functionKey,
                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput, "POST");
        return getMvcResult(result);
    }

    @Timed
    @PostMapping(value = "/functions/mvc/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.MVC.CALL')")
    @PrivilegeDescription("Privilege to execute a mvc function by key (key in entity specification)")
    public ModelAndView callPostFormMvcFunction(@PathVariable("functionKey") String functionKey,
                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return callMvcFunction(functionKey, functionInput);
    }

    @Timed
    @PostMapping("/functions/anonymous/mvc/{functionKey:.+}")
    public ModelAndView callMvcAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                 @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput, "POST");
        return getMvcResult(result);
    }

    @Timed
    @GetMapping("/functions/anonymous/mvc/{functionKey:.+}")
    public ModelAndView callMvcGetAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                 @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput, "GET");
        return getMvcResult(result);
    }

    @Timed
    @PostMapping(value = "/functions/anonymous/mvc/{functionKey:.+}", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFormMvcAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                                         @RequestParam(required = false) Map<String, Object> functionInput) {
        return callMvcAnonymousFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/**")
    public ResponseEntity<Object> callGetFunction(HttpServletRequest request,
                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callGetFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/**")
    public ResponseEntity<Object> callPostFunction(HttpServletRequest request,
                                                   @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPostFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormFunction(HttpServletRequest request,
                                                       @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callPostFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PutMapping("/functions/**")
    public ResponseEntity<Object> callPutFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPutFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PatchMapping("/functions/**")
    public ResponseEntity<Object> callPatchFunction(HttpServletRequest request,
                                                    @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPatchFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @DeleteMapping("/functions/**")
    public ResponseEntity<Object> callDeleteFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callDeleteFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/mvc/**")
    public ModelAndView callMvcFunction(HttpServletRequest request,
                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callMvcFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/mvc/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFormMvcFunction(HttpServletRequest request,
                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callMvcFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/anonymous/mvc/**")
    public ModelAndView callMvcAnonymousGetFunction(HttpServletRequest request,
                                                    @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callMvcGetAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/anonymous/mvc/**")
    public ModelAndView callMvcAnonymousFunction(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callMvcAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/anonymous/mvc/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFromMvcAnonymousFunction(HttpServletRequest request,
                                                         @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callMvcAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/anonymous/**")
    public ResponseEntity<Object> callGetAnonymousFunction(HttpServletRequest request,
                                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callGetAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/anonymous/**")
    public ResponseEntity<Object> callPostAnonymousFunction(HttpServletRequest request,
                                                            @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPostAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/anonymous/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ResponseEntity<Object> callPostFormAnonymousFunction(HttpServletRequest request,
                                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callPostAnonymousFunction(getFunctionKey(request), functionInput);
    }

    @Timed
    @PostMapping(value = "/functions/**" + UPLOAD, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @SneakyThrows
    @PrivilegeDescription("Privilege to call upload function")
    public ResponseEntity<Object> callUploadFunction(HttpServletRequest request,
                                                     @RequestParam(value = "file", required = false) List<MultipartFile> files,
                                                     HttpServletRequest httpServletRequest) {
        Map<String, Object> functionInput = of("httpServletRequest", httpServletRequest, "files", files);
        String functionKey = getFunctionKey(request);
        functionKey = functionKey.substring(0, functionKey.length() - UPLOAD.length());
        FunctionContext result = functionService.execute(functionKey, functionInput, "POST");
        return ResponseEntity.ok().body(result.functionResult());
    }

    public static String getFunctionKey(HttpServletRequest request) {
        String path = request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        return new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
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

    private ModelAndView getMvcResult(FunctionContext context) {
        if (context == null || context.getData() == null) {
            return null;
        }

        Object modelAndView = context.getData().get(MVC_FUNC_RESULT);
        if (modelAndView instanceof ModelAndView) {
            return (ModelAndView) modelAndView;
        }

        log.warn("Context did not contain {} or type (ModelAndView) mismatch, present keys {}", MVC_FUNC_RESULT, context.getData().keySet());

        return null;
    }

}
