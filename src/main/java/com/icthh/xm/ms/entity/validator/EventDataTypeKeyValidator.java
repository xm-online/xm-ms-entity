package com.icthh.xm.ms.entity.validator;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.EventSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import java.util.Optional;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EventDataTypeKeyValidator implements ConstraintValidator<EventDataTypeKey, Event> {

    private static final String TYPE_KEY_FIELD_NAME = "typeKey";

    private final XmEntitySpecService xmEntitySpecService;

    @Override
    public boolean isValid(Event event, ConstraintValidatorContext constraintValidatorContext) {
        XmEntity eventDataRef = event.getEventDataRef();
        if (eventDataRef == null) {
            return true;
        }

        String eventTypeKey = event.getTypeKey();
        Optional<EventSpec> eventSpec = xmEntitySpecService.findEvent(eventTypeKey);
        if (eventSpec.isEmpty()) {
            processConstraintViolation(constraintValidatorContext,
                "Event specification not found by key: " + eventTypeKey, TYPE_KEY_FIELD_NAME);
            return false;
        }

        String eventSpecDataTypeKey = eventSpec.get().getDataTypeKey();
        if (isBlank(eventSpecDataTypeKey)) {
            processConstraintViolation(constraintValidatorContext,
                "Data type key not configured for Event with type key: " + eventTypeKey, TYPE_KEY_FIELD_NAME);
            return false;
        }

        if (xmEntitySpecService.getTypeSpecByKeyWithoutFunctionFilter(eventSpecDataTypeKey).isEmpty()) {
            processConstraintViolation(constraintValidatorContext,
                "Type specification not found by key: " + eventSpecDataTypeKey, TYPE_KEY_FIELD_NAME);
            return false;
        }

        if (!eventSpecDataTypeKey.equals(eventDataRef.getTypeKey())) {
            processConstraintViolation(constraintValidatorContext,
                "Specified event data ref type key not matched with configured", "eventDataRef");
            return false;
        }
        return true;
    }

    private void processConstraintViolation(ConstraintValidatorContext constraintValidatorContext,
                                            String message,
                                            String propertyNode) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(propertyNode)
            .addConstraintViolation();
    }
}
