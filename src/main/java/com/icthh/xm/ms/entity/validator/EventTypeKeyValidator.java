package com.icthh.xm.ms.entity.validator;

import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.spec.CalendarSpec;
import com.icthh.xm.ms.entity.domain.spec.EventSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Slf4j
public class EventTypeKeyValidator implements ConstraintValidator<EventTypeKey, Event> {

    @Autowired
    private XmEntitySpecService xmEntitySpecService;

    @Override
    public void initialize(EventTypeKey constraintAnnotation) {
        log.trace("Event typekey validator inited");
    }

    @Override
    public boolean isValid(Event value, ConstraintValidatorContext context) {



        if (value.getCalendar() == null) {
            List<TypeSpec> typeSpecs = xmEntitySpecService.findAllTypes();
            return typeSpecs.stream().filter(spec -> spec.getCalendars() != null).flatMap(typeSpec -> typeSpec.getCalendars().stream())
                .filter(calend -> calend.getEvents() != null)
                .flatMap(calendar -> calendar.getEvents().stream()).anyMatch(it -> it.getKey().equals(value.getTypeKey()));

        }else {
            TypeSpec typeSpec = xmEntitySpecService.findTypeByKey(value.getCalendar().getXmEntity().getTypeKey());
            List<CalendarSpec> calendarSpecs = typeSpec.getCalendars();
            return calendarSpecs.stream().flatMap(calendarSpec -> calendarSpec.getEvents().stream()).anyMatch(it -> it.getKey().equals(value.getTypeKey()));

        }
    }
}
