package com.icthh.xm.ms.entity.web.rest.error;

import com.google.common.base.Throwables;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
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
import org.springframework.web.bind.annotation.ResponseStatus;

import java.sql.SQLException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class EntityExceptionTranslator {

    private static final String unique_violation_error_code = "23005";
    private static final String unique_violation_message_code = "error.unique.constrain";

    private final LocalizationMessageService localizationMessageService;
    private final ExceptionTranslator exceptionTranslator;


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<ErrorVM> processParameterizedValidationError(DataIntegrityViolationException dataIntegrityViolationException) {
        Throwable root = Throwables.getRootCause(dataIntegrityViolationException);
        if (root instanceof SQLException) {
           if (unique_violation_error_code.equalsIgnoreCase((((SQLException) root)).getSQLState())){
               return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorVM(unique_violation_message_code,
                   localizationMessageService
                       .getMessage(unique_violation_message_code)));
            }
        }
        return exceptionTranslator.processException(dataIntegrityViolationException);
    }

}
