package com.icthh.xm.ms.entity.service.xm;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.EntityApp;
import com.icthh.xm.ms.entity.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.ms.entity.config.tenant.TenantContext;
import com.icthh.xm.ms.entity.config.tenant.WebappTenantOverrideConfiguration;
import com.icthh.xm.ms.entity.domain.Location;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LocationSpec;
import com.icthh.xm.ms.entity.domain.spec.TagSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.service.XmEntityGeneratorService;
import com.icthh.xm.ms.entity.service.XmEntitySpecService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EntityApp.class, SecurityBeanOverrideConfiguration.class, WebappTenantOverrideConfiguration.class})
public class XmEntityGeneratorServiceIntTest {

    private static final String ENTITY_TYPE_WITH_TAGS_AND_LOCATIONS_KEY = "TYPE1.SUBTYPE1";

    private XmEntityGeneratorService xmEntityGeneratorService;
    private com.icthh.xm.ms.entity.service.XmEntityServiceImpl entityServiceMock;

    @Autowired
    private XmEntitySpecService xmEntitySpecService;
    @Autowired
    private Validator validator;

    @Before
    public void init() {
        entityServiceMock = mock(com.icthh.xm.ms.entity.service.XmEntityServiceImpl.class);
        when(entityServiceMock.save(any())).thenAnswer(arg -> arg.getArguments()[0]);
        xmEntityGeneratorService = new XmEntityGeneratorService(entityServiceMock, xmEntitySpecService);
        TenantContext.setCurrent("TEST");
    }

    @After
    public void finalize() {
        TenantContext.setCurrent("XM");
    }

    @Test
    @SneakyThrows
    public void generateXmEntityWithTagsTest() {
        XmEntity generatedEntity = xmEntityGeneratorService.generateXmEntity(ENTITY_TYPE_WITH_TAGS_AND_LOCATIONS_KEY);

        log.info(new ObjectMapper().writeValueAsString(generatedEntity));

        Set<Tag> tags = generatedEntity.getTags();

        assertFalse("No tags generated", isEmpty(tags));

        TypeSpec specType = xmEntitySpecService.findTypeByKey("TYPE1.SUBTYPE1");
        List<TagSpec> tagsSpecs = specType.getTags();

        Set<String> specTagsKeys = tagsSpecs.stream().map(TagSpec::getKey).collect(toSet());
        Set<String> tagsKeys = tags.stream().map(Tag::getTypeKey).collect(toSet());

        assertTrue("Tag type not from tag specification", specTagsKeys.containsAll(tagsKeys));

        for (val tag : tags) {
            assertFalse("Name of tag is empty", isBlank(tag.getName()));
        }
    }

    @Test
    @SneakyThrows
    public void generateXmEntityWithLocations() {
        XmEntity generatedEntity = xmEntityGeneratorService.generateXmEntity(ENTITY_TYPE_WITH_TAGS_AND_LOCATIONS_KEY);

        log.info(new ObjectMapper().writeValueAsString(generatedEntity));

        Set<Location> locations = generatedEntity.getLocations();

        assertFalse("No locations generated", isEmpty(locations));

        TypeSpec specType = xmEntitySpecService.findTypeByKey("TYPE1.SUBTYPE1");
        List<LocationSpec> locationSpecs = specType.getLocations();

        Set<String> locationSpecKeys = locationSpecs.stream().map(LocationSpec::getKey).collect(toSet());
        Set<String> locationKeys = locations.stream().map(Location::getTypeKey).collect(toSet());

        assertTrue("Tag type not from locations specification", locationSpecKeys.containsAll(locationKeys));

        for (val location : locations) {
            assertFalse("Name of location is empty", isBlank(location.getName()));
            assertFalse("Coordinates and address is empty", isBlank(location.getAddressLine1())
                && (location.getLatitude() == null || location.getLongitude() == null));
        }
    }

    @Test
    @SneakyThrows
    public void generateXmEntityWithRequiredDataFields() {
        XmEntity generatedEntity = xmEntityGeneratorService.generateXmEntity("TYPE3");
        log.info(new ObjectMapper().writeValueAsString(generatedEntity));

        Set<ConstraintViolation<XmEntity>> constraintViolations = validator.validate(generatedEntity);
        assertEquals(0, constraintViolations.size());
    }

}
