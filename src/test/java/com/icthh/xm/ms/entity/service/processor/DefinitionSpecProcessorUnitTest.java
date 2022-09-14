package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.spec.DefinitionSpec;
import com.icthh.xm.ms.entity.domain.spec.FormSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.JsonListenerService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class DefinitionSpecProcessorUnitTest extends AbstractUnitTest {

    private static final String ENTITY_APP_NAME = "entity";
    private static final String TENANT = "XM";
    private static final String PATH_TO_FILE = "/config/tenants/XM/entity/xmentityspec/definitions/specification-definitions.json";
    private DefinitionSpecProcessor subject;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        JsonListenerService jsonListenerService = new JsonListenerService(ENTITY_APP_NAME);
        subject = new DefinitionSpecProcessor(jsonListenerService);
        objectMapper = new ObjectMapper();
        jsonListenerService.onRefresh(PATH_TO_FILE, loadFile("config/specs/definitions/specification-definitions.json"));
    }

    @Test
    public void shouldProcessTypeSpecWithSingleDefinition() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-input");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-input-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.updateDefinitionStateByTenant(TENANT, Map.of(TENANT, Map.of(TENANT, objectMapper.writeValueAsString(inputXmEntitySpec))));
        subject.processTypeSpec(TENANT, typeSpec::setDataSpec, typeSpec::getDataSpec);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @Test
    public void shouldProcessTypeSpecIfRecursion() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-recursive");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-recursive-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.updateDefinitionStateByTenant(TENANT, Map.of(TENANT, Map.of(TENANT, objectMapper.writeValueAsString(inputXmEntitySpec))));
        subject.processTypeSpec(TENANT, typeSpec::setDataSpec, typeSpec::getDataSpec);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @Test
    public void shouldProcessTypeSpecWithMultipleDefinitions() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-multiple");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-multiple-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.updateDefinitionStateByTenant(TENANT, Map.of(TENANT, Map.of(TENANT, objectMapper.writeValueAsString(inputXmEntitySpec))));
        subject.processTypeSpec(TENANT, typeSpec::setDataSpec, typeSpec::getDataSpec);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @SneakyThrows
    private XmEntitySpec loadXmEntitySpecByFileName(String name) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(loadFile("config/specs/definitions/" + name + ".yml"), XmEntitySpec.class);
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

    @SneakyThrows
    private void assertEqualsEntities(XmEntitySpec expected, XmEntitySpec actual) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        TypeSpec expectedTypeSpec = expected.getTypes().get(0);
        TypeSpec actualTypeSpec = actual.getTypes().get(0);
        Map expectedTree = objectMapper.readValue(expectedTypeSpec.getDataSpec(), Map.class);
        Map actualTree = objectMapper.readValue(actualTypeSpec.getDataSpec(), Map.class);

        assertEquals(expectedTree, actualTree);
        assertEquals(expectedTypeSpec.getKey(), actualTypeSpec.getKey());
        assertEquals(expected.getDefinitions(), actual.getDefinitions());
        assertEquals(expected.getForms(), actual.getForms());
    }
}
