package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.domain.DefinitionSpec;
import com.icthh.xm.commons.domain.FormSpec;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.ms.entity.service.json.JsonConfigurationListener.XM_ENTITY_SPEC_KEY;
import static com.icthh.xm.ms.entity.web.rest.XmEntitySaveIntTest.loadFile;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
public class XmEntityDefinitionSpecProcessorUnitTest extends AbstractJupiterUnitTest {

    private static final String ENTITY_APP_NAME = "entity";
    private static final String TENANT = "XM";
    private static final String RELATIVE_PATH_TO_FILE = "xmentityspec/definitions/specification-definitions.json";
    private XmEntityDefinitionSpecProcessor subject;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        JsonListenerService jsonListenerService = new JsonListenerService();
        XmEntityTypeSpecProcessor typeSpecProcessor = new XmEntityTypeSpecProcessor(jsonListenerService);
        subject = new XmEntityDefinitionSpecProcessor(jsonListenerService, typeSpecProcessor);
        objectMapper = new ObjectMapper();
        jsonListenerService.processTenantSpecification(TENANT, RELATIVE_PATH_TO_FILE, loadFile("config/specs/definitions/specification-definitions.json"));
    }

    @Test
    public void shouldProcessTypeSpecWithSingleDefinition() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-input");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-input-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getDefinitions());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataSpec, typeSpec::getDataSpec);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @Test
    public void shouldProcessTypeSpecRefInDefinition() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-sub-definitions-input");
        String expectedXmEntityDataSpec = loadFile("config/specs/definitions/xmentityspec-sub-definitions-input-expected.json");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().stream().filter(it -> it.getKey().equals("REQUEST.JOIN")).findFirst().get();

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getDefinitions());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataSpec, typeSpec::getDataSpec);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        objectMapper = objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        Map actualMap = objectMapper.readValue(actualXmEntitySpec.getTypes().get(0).getDataSpec(), Map.class);
        Map expectedMap = objectMapper.readValue(expectedXmEntityDataSpec, Map.class);

        assertEquals(objectMapper.writeValueAsString(expectedMap), objectMapper.writeValueAsString(actualMap));
    }

    @Test
    public void shouldProcessTypeSpecIfRecursion() throws Exception {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-recursive");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-recursive-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getDefinitions());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataSpec, typeSpec::getDataSpec);

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

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getDefinitions());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataSpec, typeSpec::getDataSpec);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);
    }

    @Test
    public void shouldProcessTypeSpecAndAfterRemoveDefGetCorrectDefinitions() {
        XmEntitySpec inputXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-multiple");
        XmEntitySpec expectedXmEntitySpec = loadXmEntitySpecByFileName("xmentityspec-definitions-multiple-expected");
        TypeSpec typeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getDefinitions());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, typeSpec::setDataSpec, typeSpec::getDataSpec);
        subject.processDefinitionsItSelf(TENANT, XM_ENTITY_SPEC_KEY);

        XmEntitySpec actualXmEntitySpec = createXmEntitySpec(singletonList(typeSpec),
            inputXmEntitySpec.getForms(),
            inputXmEntitySpec.getDefinitions());

        assertEqualsEntities(expectedXmEntitySpec, actualXmEntitySpec);

        Collection<DefinitionSpec> specDefinitions = subject.getProcessedSpecsCopy(TENANT, XM_ENTITY_SPEC_KEY);
        assertEquals(2, specDefinitions.size());

        inputXmEntitySpec.getDefinitions().removeLast(); //remove last definition
        TypeSpec updateTypeSpec = inputXmEntitySpec.getTypes().get(0);

        subject.fullUpdateStateByTenant(TENANT, XM_ENTITY_SPEC_KEY, inputXmEntitySpec.getDefinitions());
        subject.processDataSpec(TENANT, XM_ENTITY_SPEC_KEY, updateTypeSpec::setDataSpec, updateTypeSpec::getDataSpec);
        subject.processDefinitionsItSelf(TENANT, XM_ENTITY_SPEC_KEY);

        List<DefinitionSpec> updatedSpecDefinitions = subject.getProcessedSpecsCopy(TENANT, XM_ENTITY_SPEC_KEY).stream().toList();

        assertEquals(1, updatedSpecDefinitions.size());
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
