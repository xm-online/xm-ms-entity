package com.icthh.xm.ms.entity.web.rest;

import static com.google.common.collect.ImmutableMap.of;
import static com.icthh.xm.commons.permission.constants.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.ms.entity.security.SecurityUtils.getCurrentUserRole;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.web.rest.util.HeaderUtil;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public static final String FUNCTION_SOURCE_CODE = "functionSourceCode";

    private final FunctionService functionService;

    public FunctionResource(@Qualifier("functionService") FunctionService functionService) {
        this.functionService = functionService;
    }

    @Timed
    @GetMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.GET.CALL')")
    public ResponseEntity<Object> callGetFunction(@PathVariable("functionKey") String functionKey,
                                                           @RequestParam(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput);
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PutMapping("/functions/{functionKey:.+}")
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.PUT.CALL')")
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
    public ResponseEntity<Object> callFunction(@PathVariable("functionKey") String functionKey,
                                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        FunctionContext result = functionService.execute(functionKey, functionInput);
        return ResponseEntity.created(URI.create("/api/function-contexts/" + result.getId()))
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
    @PostMapping(value = "/functions/{functionKey:.+}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @SneakyThrows
    public ResponseEntity<Object> callUploadFunction(@PathVariable("functionKey") String functionKey,
                                                     @RequestParam(value = "file", required = false) List<MultipartFile> files,
                                                     HttpServletRequest httpServletRequest) {
        Map<String, Object> functionInput = of("httpServletRequest", httpServletRequest, "files", files);
        FunctionContext result = functionService.execute(functionKey, functionInput);
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PostMapping(value = "/functions/system/evaluate")
    @SneakyThrows
    public ResponseEntity<Object> evaluateOneTimeFunction(@RequestBody Map<String, Object> functionInput) {
        assertIsSuperAdmin();
        String functionSourceCode = String.valueOf(functionInput.remove(FUNCTION_SOURCE_CODE));
        FunctionContext result = functionService.evaluate(functionSourceCode, functionInput);
        return ResponseEntity.ok().body(result.functionResult());
    }

    @Timed
    @PostMapping(value = "/functions/{idOrKey}/system/evaluate")
    @SneakyThrows
    public ResponseEntity<Object> evaluateOneTimeFunction(@PathVariable("idOrKey") IdOrKey idOrKey,
                                                          @RequestBody Map<String, Object> functionInput) {
        assertIsSuperAdmin();
        String functionSourceCode = String.valueOf(functionInput.remove(FUNCTION_SOURCE_CODE));
        FunctionContext result = functionService.evaluate(functionSourceCode, idOrKey, functionInput);
        return ResponseEntity.ok().body(result.functionResult());
    }

    private void assertIsSuperAdmin() {
        getCurrentUserRole().filter(SUPER_ADMIN::equals)
            .orElseThrow(() -> new AccessDeniedException("Evaluate one time function can only super admin"));
    }
}
