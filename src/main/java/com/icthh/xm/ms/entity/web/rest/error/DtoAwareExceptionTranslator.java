package com.icthh.xm.ms.entity.web.rest.error;

import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.domain.vm.ErrorVM;
import com.icthh.xm.commons.i18n.error.domain.vm.FieldErrorVM;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.commons.i18n.spring.service.LocalizationMessageService;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * Extends the commons ExceptionTranslator to strip "Dto" suffix from validation
 * error objectName for backward compatibility after DTO migration.
 */
@ControllerAdvice
@Primary
public class DtoAwareExceptionTranslator extends ExceptionTranslator {

    private static final String DTO_SUFFIX = "Dto";

    private final LocalizationMessageService localizationMessageService;

    public DtoAwareExceptionTranslator(LocalizationMessageService localizationMessageService) {
        super(localizationMessageService);
        this.localizationMessageService = localizationMessageService;
    }

    @Override
    public ErrorVM processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        FieldErrorVM dto = new FieldErrorVM(ErrorConstants.ERR_VALIDATION,
            localizationMessageService.getMessage(ErrorConstants.ERR_VALIDATION));
        for (FieldError fieldError : result.getFieldErrors()) {
            dto.add(stripDtoSuffix(fieldError.getObjectName()), fieldError.getField(),
                fieldError.getCode(), fieldError.getDefaultMessage());
        }
        for (ObjectError globalError : result.getGlobalErrors()) {
            dto.add(stripDtoSuffix(globalError.getObjectName()), globalError.getObjectName(),
                globalError.getCode(), globalError.getDefaultMessage());
        }
        return dto;
    }

    private static String stripDtoSuffix(String objectName) {
        if (objectName != null && objectName.endsWith(DTO_SUFFIX)) {
            return objectName.substring(0, objectName.length() - DTO_SUFFIX.length());
        }
        return objectName;
    }
}
