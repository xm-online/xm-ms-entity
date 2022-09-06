package com.icthh.xm.ms.entity.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.spec.FormSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.XmEntitySpec;
import com.icthh.xm.ms.entity.service.JsonListenerService;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FormSpecProcessorUnitTest extends AbstractUnitTest {
    private static final String ENTITY_APP_NAME = "entity";
    private static final String TENANT = "XM";
    private static final String PATH_TO_FILE = "/config/tenants/XM/entity/xmentityspec/forms/user.json";
    @Mock private JsonListenerService jsonListenerService;
    @InjectMocks
    private FormSpecProcessor subject;

    private ObjectMapper objectMapper;

    @Before
    public void setUp(){
        objectMapper = new ObjectMapper();



    }

    @Test
    public void shouldProcessDataSpec_withSingleReferences_success() throws Exception{
        TypeSpec typeSpec = new TypeSpec();
        typeSpec.setDataForm("{\"form\":[{\"$ref\":\"#/xmEntityForm/requestform\"}]}");
        FormSpec formSpec = new FormSpec();
        formSpec.setKey("requestform");
        formSpec.setRef("xmentityspec/forms/user.json");

        String jsonSpecification = "{\"key\":\"test\", \"value\": \"test\"}";
        String expectedForm = "{\"form\":[{\"key\":\"test\",\"value\":\"test\"}]}";

        XmEntitySpec xmEntitySpec = createXmEntitySpec(singletonList(typeSpec), singletonList(formSpec));

        when(jsonListenerService.getSpecificationByTenantRelativePath(TENANT, formSpec.getRef())).thenReturn(jsonSpecification);

        subject.updateFormStateByTenant(TENANT, Map.of(TENANT, Map.of(TENANT, objectMapper.writeValueAsString(xmEntitySpec))));
        TypeSpec actual = subject.processTypeSpec(TENANT, typeSpec);

        assertThat(actual.getDataForm()).isEqualTo(expectedForm);

        verify(jsonListenerService).getSpecificationByTenantRelativePath(eq(TENANT),anyString());
        verifyNoMoreInteractions(jsonListenerService);
    }

    @SneakyThrows
    @Test
    public void test() {
        TypeSpec typeSpec = new TypeSpec();
        typeSpec.setDataForm(" {         \"form\": [\n" +
            "            {\"key\": \"field5\"},\n" +
            "            {\"key\": \"object1.field6\"},\n" +
            "            {\"$ref\":  \"#/xmEntityForm/userform\"}\n" +
            "          ]\n}");
        FormSpec formSpec = new FormSpec();
        formSpec.setKey("userform");
        formSpec.setRef("xmentityspec/forms/user.json");

        jsonListenerService.onRefresh(PATH_TO_FILE, " {         \"form\": [\n" +
            "            {\"key\": \"jsonFileField\"},\n" +
            "            {\"key\": \"object1.jsonFileField6\"}\n" +
            "          ]\n}");
//        subject.updateFormStateByTenant("XM", Map.of("XM", Map.of("XM", objectMapper.writeValueAsString(xmEntitySpec))));
        System.out.println(subject.processTypeSpec("XM", typeSpec).getDataForm());
    }

    private XmEntitySpec createXmEntitySpec(List<TypeSpec> typeSpecs, List<FormSpec> formSpecs){
        XmEntitySpec xmEntitySpec = new XmEntitySpec();
        xmEntitySpec.setTypes(typeSpecs);
        xmEntitySpec.setForms(formSpecs);
        return  xmEntitySpec;
    }
}
