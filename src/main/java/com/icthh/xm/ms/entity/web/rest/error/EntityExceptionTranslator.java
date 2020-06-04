package com.icthh.xm.ms.entity.web.rest.error;

import com.google.common.base.Throwables;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.ParameterizedErrorVM;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;
import java.util.Map;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class EntityExceptionTranslator {

    private static final String INTEGRITY_CONSTRAINT_VIOLATION_GROUP_CODE = "23";
    private static final String INTEGRITY_CONSTRAINT_VIOLATION_ERROR_CODE_PREFIX = "error.db.dataIntegrityViolation.";

    private final LocalizationMessageService localizationMessageService;
    private final ExceptionTranslator exceptionTranslator;
    private final LepExceptionParametersResolver lepExceptionParametersResolver;


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResponseEntity<ParameterizedErrorVM> processParameterizedValidationError(DataIntegrityViolationException dataIntegrityViolationException) {
        Throwable root = Throwables.getRootCause(dataIntegrityViolationException);
        String defaultMessage = root.getMessage();
        if (root instanceof SQLException) {
            String sqlState = ((SQLException) root).getSQLState();
            if (sqlState != null && sqlState.startsWith(INTEGRITY_CONSTRAINT_VIOLATION_GROUP_CODE)) {
                String errorCode = INTEGRITY_CONSTRAINT_VIOLATION_ERROR_CODE_PREFIX.concat(sqlState);
                Map<String, String> paramsMap = lepExceptionParametersResolver.extractParameters(root);
                String message = localizationMessageService.getMessage(errorCode, paramsMap, false, defaultMessage);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(buildParameterizedErrorVM(errorCode, message, paramsMap));
            }
        }
        ResponseEntity<ErrorVM> responseEntity = exceptionTranslator.processException(dataIntegrityViolationException);
        ErrorVM errorVM = responseEntity.getBody();
        Map<String, String> paramsMap = lepExceptionParametersResolver.extractParameters(root);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildParameterizedErrorVM(errorVM, paramsMap));
    }


    private ParameterizedErrorVM buildParameterizedErrorVM(ErrorVM errorVM, Map<String, String> params) {
        String error = errorVM == null ? "error.missed.exception.translator" : errorVM.getError();
        String errorDescription = errorVM == null ? "Unable to translate exception" : errorVM.getError_description();
        return buildParameterizedErrorVM(error, errorDescription, params);
    }

    private ParameterizedErrorVM buildParameterizedErrorVM(final String error, final String errorDescription, Map<String, String> params) {
        return new ParameterizedErrorVM(error, errorDescription, params);
    }

}
