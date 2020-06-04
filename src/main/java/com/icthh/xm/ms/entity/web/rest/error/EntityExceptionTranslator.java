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

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class EntityExceptionTranslator {

    private static final String INTEGRITY_CONSTRAINT_VIOLATION_GROUP_CODE = "23";
    private static final String INTEGRITY_CONSTRAINT_VIOLATION_MESSAGE_CODE = "error.db.dataIntegrityViolation.";
    private static final String INTEGRITY_CONSTRAINT_VIOLATION_ERROR_CODE_PREFIX = "error.db.";

    private final LocalizationMessageService localizationMessageService;
    private final ExceptionTranslator exceptionTranslator;
    private final LepExceptionParametersResolver lepExceptionParametersResolver;


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResponseEntity<ParameterizedErrorVM> processParameterizedValidationError(DataIntegrityViolationException dataIntegrityViolationException) {
        Throwable root = Throwables.getRootCause(dataIntegrityViolationException);
        if (root instanceof SQLException) {
            String sqlState = ((SQLException) root).getSQLState();
            if (sqlState != null && sqlState.startsWith(INTEGRITY_CONSTRAINT_VIOLATION_GROUP_CODE)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ParameterizedErrorVM(
                    INTEGRITY_CONSTRAINT_VIOLATION_ERROR_CODE_PREFIX.concat(sqlState),
                    localizationMessageService
                        .getMessage(INTEGRITY_CONSTRAINT_VIOLATION_MESSAGE_CODE.concat(sqlState)),
                    lepExceptionParametersResolver.extractParameters(root)));
            }
        }
        ResponseEntity<ErrorVM> responseEntity = exceptionTranslator.processException(dataIntegrityViolationException);
        ErrorVM errorVM = responseEntity.getBody();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ParameterizedErrorVM(
            errorVM == null ? "error.missed.exception.translator" : errorVM.getError(),
            errorVM == null ? "Unable to translate exception" : errorVM.getError_description(),
            lepExceptionParametersResolver.extractParameters(dataIntegrityViolationException)));
    }


}
