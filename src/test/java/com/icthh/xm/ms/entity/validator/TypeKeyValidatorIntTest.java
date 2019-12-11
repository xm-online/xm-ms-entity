package com.icthh.xm.ms.entity.validator;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.ms.entity.AbstractSpringBootTest;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

@Slf4j
public class TypeKeyValidatorIntTest extends AbstractSpringBootTest {

    @Autowired
    private Validator validator;
    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, "TEST");
    }

    @After
    public void tearDown() {
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

    @SneakyThrows
    private void testDataValidation(Map data, String actualType, List<String> expected) {
        XmEntity entity = new XmEntity().key("TYPE1.SUBTYPE1-1").typeKey("TYPE1.SUBTYPE1").name("Entity name")
            .startDate(Instant.now()).updateDate(Instant.now()).stateKey("STATE1").data(data);
        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(entity);

        assertEquals(1, constraintViolations.size());
        String messageTemplate = constraintViolations.iterator().next().getMessageTemplate();
        Map<?, ?> errorData = (Map<?, ?>) new ObjectMapper().readValue(messageTemplate, List.class).get(0);
        assertEquals("validation", errorData.get("domain"));
        assertEquals("type", errorData.get("keyword"));
        assertEquals(actualType, errorData.get("found"));
        assertEquals(expected, errorData.get("expected"));
    }

    @Test
    public void testXmEntityDataValidationIsInvalidBoolean() {
        Map data = of("booleanProperties", "false");
        testDataValidation(data, "string", singletonList("boolean"));
    }


    @Test
    public void testXmEntityDataValidationIsInvalidNumber() {
        Map data = of("numberProperties", "57");
        testDataValidation(data, "string", asList("integer", "number"));
    }

    @Test
    public void testXmEntityDataValidationIsInvalidString() {
        Map data = of("stringProperties", 57);
        testDataValidation(data, "integer", singletonList("string"));
    }

}
