package com.icthh.xm.ms.entity.validator;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.EventSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EventDataTypeKeyValidator implements ConstraintValidator<EventDataTypeKey, Event> {

    private final XmEntitySpecService xmEntitySpecService;

    @Override
    public boolean isValid(Event event, ConstraintValidatorContext constraintValidatorContext) {
        XmEntity eventDataRef = event.getEventDataRef();
        if (eventDataRef == null) {
            return true;
        }

        String eventTypeKey = event.getTypeKey();
        EventSpec eventSpec = xmEntitySpecService.findEvent(eventTypeKey)
            .orElseThrow(() -> new EntityNotFoundException("Event specification not found by key: " + eventTypeKey));

        String eventSpecDataTypeKey = eventSpec.getDataTypeKey();
        if (isBlank(eventSpecDataTypeKey)) {
            throw new IllegalStateException("Data type key not configured for Event with type key: "
                + eventSpec.getKey());
        }

        if (xmEntitySpecService.getTypeSpecByKey(eventSpecDataTypeKey).isEmpty()) {
            throw new EntityNotFoundException("Type specification not found by key: " + eventSpecDataTypeKey);
        }

        return eventSpecDataTypeKey.equals(eventDataRef.getTypeKey());
    }
}
