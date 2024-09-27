package com.icthh.xm.ms.entity.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.service.FunctionService;
import com.icthh.xm.ms.entity.web.rest.util.FunctionUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

/**
 * The {@link FunctionUploadResource} class.
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class FunctionUploadResource {

    private static final String UPLOAD = "/upload";

    private final FunctionService functionService;

    public FunctionUploadResource(@Qualifier("functionService") FunctionService functionService) {
        this.functionService = functionService;
    }

    @Timed
    @PostMapping(value = "/functions/**", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission({'functionKey': #functionKey}, 'FUNCTION.UPLOAD.CALL')")
    @SneakyThrows
    @PrivilegeDescription("Privilege to call upload function")
    public ResponseEntity<Object> callUploadFunction(HttpServletRequest request,
                                                     @RequestParam(value = "file", required = false) List<MultipartFile> files,
                                                     HttpServletRequest httpServletRequest) {
        if (!request.getRequestURI().endsWith(UPLOAD)) {
            return ResponseEntity.badRequest().body("Invalid upload url");
        }
        Map<String, Object> functionInput = of("httpServletRequest", httpServletRequest, "files", files);
        String functionKey = FunctionUtils.getFunctionKey(request);
        functionKey = functionKey.substring(0, functionKey.length() - UPLOAD.length());
        FunctionContext result = functionService.execute(functionKey, functionInput, "POST");
        return ResponseEntity.ok().body(result.functionResult());
    }

}
