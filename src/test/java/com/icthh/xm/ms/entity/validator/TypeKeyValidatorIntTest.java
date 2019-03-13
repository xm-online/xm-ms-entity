package com.icthh.xm.ms.entity.validator;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Calendar;
import com.icthh.xm.ms.entity.domain.Event;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class TypeKeyValidatorIntTest {

    @Autowired
    private Validator validator;
    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
    }

    @After
    @Override
    public void finalize() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void testEntityTypeKeyValidationIsValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
                .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1");
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(0, constraintViolations.size());
    }

    @Test
    public void testEntityTypeKeyValidationIsNotValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE2-1").typeKey("TYPE1.SUBTYPE2").name("Entity name")
                .startDate(Instant.now()).updateDate(Instant.now());
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(1, constraintViolations.size());
    }

    @Test
    public void testTagTypeKeyValidationIsValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
                .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1")
                .addTags(new Tag().typeKey("TAG1").name("Tag").startDate(Instant.now()));
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(0, constraintViolations.size());
    }

    @Test
    public void testTagTypeKeyValidationIsNotValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
                .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1")
                .addTags(new Tag().typeKey("TAG2").name("Tag").startDate(Instant.now()));
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(1, constraintViolations.size());
    }


    @Test
    public void testStateKeyValidationIsValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1");
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(0, constraintViolations.size());
    }

    @Test
    public void testStateKeyValidationIsNotValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("UNVALIDSTATE");
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(1, constraintViolations.size());
        assertEquals("{xm.ms.entity.state.constraint}", constraintViolations.iterator().next().getMessageTemplate());
    }

    @Test
    public void testXmEntityDataValidationIsValid() {
        Map data = of("booleanProperties", false, "numberProperties", 5, "stringProperties", "str");
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1").data(data);
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(0, constraintViolations.size());
    }

    private void testDataValidation(Map data) {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1").data(data);
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(1, constraintViolations.size());
        assertEquals("{xm.ms.entity.data.constraint}", constraintViolations.iterator().next().getMessageTemplate());
    }

    @Test
    public void testXmEntityDataValidationIsInvalidBoolean() {
        Map data = of("booleanProperties", "false");
        testDataValidation(data);
    }


    @Test
    public void testXmEntityDataValidationIsInvalidNumber() {
        Map data = of("numberProperties", "57");
        testDataValidation(data);
    }

    @Test
    public void testXmEntityDataValidationIsInvalidString() {
        Map data = of("stringProperties", 57);
        testDataValidation(data);
    }

    @Test
    public void testCalendarTypeKeyValidationIsValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1");
        Calendar calendar = new Calendar().typeKey("DEFAULT").xmEntity(entity).name("Calendar name");
        Set<ConstraintViolation<Calendar>> constraintViolations = validator.validate(calendar);

        assertEquals(0,constraintViolations.size());
    }


    @Test
    public void testCalendarTypeKeyValidationIsNotValid() {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1");
        Calendar calendar = new Calendar().typeKey("INVALID").xmEntity(entity).name("Calendar name");
        Set<ConstraintViolation<Calendar>> constraintViolations = validator.validate(calendar);

        assertEquals(2,constraintViolations.size());
        assertEquals("{xm.ms.entity.calendar.typekey.constraint}", constraintViolations.iterator().next().getMessageTemplate());
    }
    @Test
    public void testEventTypeKeyValidationIsValid() {
        Event event = new Event().typeKey("EVENT1").title("Event name");
        Set<ConstraintViolation<Event>> constraintViolations = validator.validate(event);

        assertEquals(0,constraintViolations.size());
    }

    @Test
    public void testEventTypeKeyValidationIsNotValid() {
        Event event = new Event().typeKey("INVALID").title("Event name");
        Set<ConstraintViolation<Event>> constraintViolations = validator.validate(event);

        assertEquals(1,constraintViolations.size());
        assertEquals("{xm.ms.entity.event.typekey.constraint}", constraintViolations.iterator().next().getMessageTemplate());
    }

}
