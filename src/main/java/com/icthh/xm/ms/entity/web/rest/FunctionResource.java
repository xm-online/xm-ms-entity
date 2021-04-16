package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

/**
 * The {@link FunctionResource} class.
 */
@RestController
@RequestMapping("/api")
public class FunctionResource {

    private static final String ENTITY_NAME_FUNCTION_CONTEXT = "functionContext";

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
        FunctionContext result = functionService.execute(functionKey, functionInput);
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PutMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.PUT.CALL')")
    @PrivilegeDescription("Privilege to call put function")
    public ResponseEntity<Object> callPutFunction(@PathVariable("functionKey") String functionKey,
                                                           @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput);
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
    public ResponseEntity<Object> callFunction(@PathVariable("functionKey") String functionKey,
                                               @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput);
        return ResponseEntity.created(URI.create("/api/function-contexts/" + Objects.toString(result.getId(), "")))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME_FUNCTION_CONTEXT, String.valueOf(result.getId())))
            .body(result.functionResult());
    }

    /**
     * POST  /functions/anonymous/{functionKey} : Execute a function by key (key in entity specification).
     *
     * @param functionKey   the function key to execute
     * @param functionInput function input data context
     * @return the ResponseEntity with status 201 (Created) and with body the new FunctionContext,
     * or with status 400 (Bad Request) if the FunctionContext has already an ID
     */
    @Timed
    @PostMapping("/functions/anonymous/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.ANONYMOUS.CALL')")
    @PrivilegeDescription("Privilege to execute a function by key (key in entity specification)")
    public ResponseEntity<Object> callAnonymousFunction(@PathVariable("functionKey") String functionKey,
                                               @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput);
        return ResponseEntity.created(URI.create("/api/function-contexts/" + Objects.toString(result.getId(), "")))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME_FUNCTION_CONTEXT, String.valueOf(result.getId())))
            .body(result.functionResult());
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
        FunctionContext result = functionService.execute(functionKey, functionInput);
        Object data = result.getData().get("modelAndView");
        if (data instanceof ModelAndView) {
            return (ModelAndView) data;
        }
        return null;
    }

    @Timed
    @GetMapping("/functions/**")
    public ResponseEntity<Object> callGetFunction(HttpServletRequest request,
                                  @RequestParam(required = false) Map<String, Object> functionInput) {
        return self.callGetFunction(getFunctionKey(request), functionInput);
    }

    @Timed
    @PostMapping("/functions/**")
    public ResponseEntity<Object> callFunction(HttpServletRequest request,
                                               @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callFunction(getFunctionKey(request), functionInput);
    }

    @Timed
    @PutMapping("/functions/**")
    public ResponseEntity<Object> callPutFunction(HttpServletRequest request,
                                                  @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callPutFunction(getFunctionKey(request), functionInput);
    }

    @Timed
    @PostMapping("/functions/mvc/**")
    public ModelAndView callMvcFunction(HttpServletRequest request,
                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        return self.callMvcFunction(getFunctionKey(request), functionInput);
    }

    @Timed
    @PostMapping(value = "/functions/{functionKey:.+}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @SneakyThrows
    @PrivilegeDescription("Privilege to call upload function")
    public ResponseEntity<Object> callUploadFunction(@PathVariable("functionKey") String functionKey,
                                                     @RequestParam(value = "file", required = false) List<MultipartFile> files,
                                                     HttpServletRequest httpServletRequest) {
        Map<String, Object> functionInput = of("httpServletRequest", httpServletRequest, "files", files);
        FunctionContext result = functionService.execute(functionKey, functionInput);
        return ResponseEntity.ok().body(result.functionResult());
    }

    public static String getFunctionKey(HttpServletRequest request) {
        String path = request.getAttribute(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString();
        String bestMatchingPattern = request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE).toString();
        return new AntPathMatcher().extractPathWithinPattern(bestMatchingPattern, path);
    }
}
