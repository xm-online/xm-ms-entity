package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.FormSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.ms.entity.service.json.JsonConfigurationListener.XM_ENTITY_SPEC_KEY;
import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class XmEntityDataFormSpecProcessorUnitTest extends AbstractUnitTest {
    private static final String TENANT = "XM";
    private static final String RELATIVE_PATH_TO_FILE = "xmentityspec/forms/specification-forms.json";
    private XmEntityDataFormSpecProcessor subject;
    private ObjectMapper objectMapper;
    private JsonListenerService jsonListenerService;

    @Before
    public void setUp() {
        jsonListenerService = new JsonListenerService();
        subject = new XmEntityDataFormSpecProcessor(jsonListenerService);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void shouldProcessTypeSpecWithSingleReferences() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-forms-input");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-forms-input-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);
        jsonListenerService.processTenantSpecification(TENANT, RELATIVE_PATH_TO_FILE, loadFile("config/specs/forms/specification-forms.json"));

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getForms());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataForm, typeSpec::getDataForm);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @Test
    public void shouldNotProcessTypeSpecIfPossibleRecursion() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-forms-recursive");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);
        jsonListenerService.processTenantSpecification(TENANT, RELATIVE_PATH_TO_FILE, loadFile("config/specs/forms/specification-forms.json"));

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getForms());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataForm, typeSpec::getDataForm);

        assertEquals(inputXmEntitySpec.getTypes().get(0).getDataForm(), typeSpec.getDataForm());
    }

    @Test
    public void shouldProcessTypeSpecWithMultipleForms() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-forms-multiple");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-forms-multiple-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);
        jsonListenerService.processTenantSpecification(TENANT, RELATIVE_PATH_TO_FILE, loadFile("config/specs/forms/specification-forms.json"));

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getForms());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataForm, typeSpec::getDataForm);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @Test(expected = JsonProcessingException.class)
    public void shouldThrowIllegalArgumentExceptionWhenProcessTypeSpecWithNotValidJson()throws JsonProcessingException {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-forms-input");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);
        jsonListenerService.processTenantSpecification(TENANT, RELATIVE_PATH_TO_FILE, "{,}");

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getForms());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataForm, typeSpec::getDataForm);
    }

    @SneakyThrows
    private XmEntitySpec loadXmEntitySpecByFileName(String name) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(loadFile("config/specs/forms/" + name + ".yml"), XmEntitySpec.class);
    }

    @SneakyThrows
    private void assertEqualsEntities(XmEntitySpec expected, XmEntitySpec actual) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        TypeSpec expectedTypeSpec = expected.getTypes().get(0);
        TypeSpec actualTypeSpec = actual.getTypes().get(0);
        Map<?, ?> expectedForm = objectMapper.readValue(expectedTypeSpec.getDataForm(), Map.class);
        Map<?, ?> actualForm = objectMapper.readValue(actualTypeSpec.getDataForm(), Map.class);

        ObjectWriter prettyPrinter = new ObjectMapper().writerWithDefaultPrettyPrinter();
        assertEquals(prettyPrinter.writeValueAsString(expectedForm), prettyPrinter.writeValueAsString(actualForm));
        assertEquals(expectedTypeSpec.getKey(), actualTypeSpec.getKey());
        assertEquals(expected.getDefinitions(), actual.getDefinitions());
        assertEquals(expected.getForms(), actual.getForms());
    }

    private XmEntitySpec createXmEntitySpec(List<TypeSpec> typeSpecs,
                                            List<FormSpec> formSpecs,
                                            List<DefinitionSpec> definitionSpecs) {
        XmEntitySpec xmEntitySpec = new XmEntitySpec();
        xmEntitySpec.setTypes(typeSpecs);
        xmEntitySpec.setDefinitions(definitionSpecs);
        xmEntitySpec.setForms(formSpecs);
        return xmEntitySpec;
    }
}
