package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.logging.aop.IgnoreLogginAspect;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.web.rest.util.FunctionUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static com.icthh.xm.ms.entity.config.Constants.MVC_FUNC_RESULT;
import static com.icthh.xm.ms.entity.service.impl.FunctionServiceImpl.POST_URLENCODED;

/**
 * The {@link FunctionMvcResource} class.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FunctionMvcResource {

    private final FunctionService functionService;

    private FunctionMvcResource self;

    public FunctionMvcResource(@Qualifier("functionService") FunctionService functionService) {
        this.functionService = functionService;
    }

    @Autowired
    public void setSelf(@Lazy FunctionMvcResource self) {
        this.self = self;
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
        FunctionContext result = functionService.executeAnonymous(functionKey, functionInput, POST_URLENCODED);
        return getMvcResult(result);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/mvc/**")
    public ModelAndView callMvcFunction(HttpServletRequest request,
                                        @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callMvcFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/mvc/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFormMvcFunction(HttpServletRequest request,
                                                @RequestParam(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callMvcFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @GetMapping("/functions/anonymous/mvc/**")
    public ModelAndView callMvcAnonymousGetFunction(HttpServletRequest request,
                                                    @RequestParam(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callMvcGetAnonymousFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping("/functions/anonymous/mvc/**")
    public ModelAndView callMvcAnonymousFunction(HttpServletRequest request,
                                                 @RequestBody(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callMvcAnonymousFunction(functionKey, functionInput);
    }

    @IgnoreLogginAspect
    @Timed
    @PostMapping(value = "/functions/anonymous/mvc/**", consumes = {
        MediaType.APPLICATION_FORM_URLENCODED_VALUE,
    })
    public ModelAndView callPostFromMvcAnonymousFunction(HttpServletRequest request,
                                                         @RequestParam(required = false) Map<String, Object> functionInput) {
        String functionKey = FunctionUtils.getFunctionKey(request);
        return self.callMvcAnonymousFunction(functionKey, functionInput);
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
