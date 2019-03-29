package com.icthh.xm.ms.entity.service;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.icthh.xm.commons.config.client.service.TenantConfigService;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Comment;
import com.icthh.xm.ms.entity.domain.FunctionContext;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.UniqueField;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class XmEntityServiceImplUnitTest extends AbstractUnitTest {

    public static final String TEST_TYPE_KEY = "TEST_TYPE_KEY";
    @InjectMocks
    private XmEntityServiceImpl xmEntityService;

    @Mock
    private XmEntitySpecService xmEntitySpecService;
    @Mock
    private XmEntityRepositoryInternal xmEntityRepository;
    @Mock
    private XmEntitySearchRepository xmEntitySearchRepository;
    @Mock
    private StartUpdateDateGenerationStrategy startUpdateDateGenerationStrategy;
    @Mock
    TenantConfigService tenantConfigService;
    @Mock
    UniqueFieldRepository uniqueFieldRepository;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void before(){
        mapper.registerModule(new JavaTimeModule());
        xmEntityService.setSelf(xmEntityService);
    }

    @Test
    public void setUniqFieldIfExists() {
        when(startUpdateDateGenerationStrategy.preProcessStartUpdateDates(any(), any(), any(), any(), any(), any()))
            .thenReturn(Optional.of(new XmEntity()));

        TypeSpec typeSpec = new TypeSpec();
        typeSpec.setUniqueFields(new HashSet<>(asList(new UniqueFieldSpec("$.uniqueExistsField"),
            new UniqueFieldSpec("$.uniqueNotExistsField"),
            new UniqueFieldSpec("$.uniqueNullField"),
            new UniqueFieldSpec("$.uniqueBlankField"),
            new UniqueFieldSpec("$.otherUniqueExistsField"),
            new UniqueFieldSpec("$.simpleObject.uniqueSubField"),
            new UniqueFieldSpec("$.uniqueObject"),
            new UniqueFieldSpec("$.uniqueObjectWithUniqueField"),
            new UniqueFieldSpec("$.uniqueObjectWithUniqueField.uniqueField"),
            new UniqueFieldSpec("$.notExistsObjectWith.uniqueField")
        )));
        when(xmEntitySpecService.findTypeByKey(TEST_TYPE_KEY)).thenReturn(typeSpec);

        XmEntity any = any();
        when(xmEntityRepository.save(any)).then(args -> args.getArguments()[0]);

        Map<String, Object> data = new HashMap<>();
        data.put("uniqueExistsField", 50);
        data.put("uniqueNullField", null);
        data.put("notUniqueField", "70");
        data.put("uniqueBlankField", "");
        data.put("otherUniqueExistsField", "25");
        data.put("simpleObject", of("uniqueSubField", "value1"));
        data.put("uniqueObject", of("notUniqueField", "value2"));
        data.put("uniqueObjectWithUniqueField", of("uniqueField", "value3"));

        XmEntity xmEntity = new XmEntity().typeKey(TEST_TYPE_KEY).data(data);

        xmEntityService.save(xmEntity);

        ArgumentCaptor<XmEntity> argument = ArgumentCaptor.forClass(XmEntity.class);
        verify(xmEntityRepository).save(argument.capture());
        Set<UniqueField> uniqueFields = argument.getValue().getUniqueFields();
        assertEquals(uniqueFields.size(), 6);
        log.info("{}", uniqueFields.stream().map(uf -> uf.getFieldJsonPath() + "|" + uf.getFieldValue()).collect(toList()));
        assertTrue(uniqueFields.contains(new UniqueField(null, "$.uniqueObject", "{\"notUniqueField\":\"value2\"}", TEST_TYPE_KEY, xmEntity)));
        assertTrue(uniqueFields.contains(new UniqueField(null, "$.uniqueExistsField", "50", TEST_TYPE_KEY, xmEntity)));
        assertTrue(uniqueFields.contains(new UniqueField(null, "$.uniqueObjectWithUniqueField", "{\"uniqueField\":\"value3\"}", TEST_TYPE_KEY, xmEntity)));
        assertTrue(uniqueFields.contains(new UniqueField(null, "$.uniqueObjectWithUniqueField.uniqueField", "value3", TEST_TYPE_KEY, xmEntity)));
        assertTrue(uniqueFields.contains(new UniqueField(null, "$.otherUniqueExistsField", "25", TEST_TYPE_KEY, xmEntity)));
        assertTrue(uniqueFields.contains(new UniqueField(null, "$.simpleObject.uniqueSubField", "value1", TEST_TYPE_KEY, xmEntity)));
    }

    @Test(expected = BusinessException.class)
    public void testFailTransitionIfLastStateAssertChangeState() {
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(null);
        xmEntityService.assertStateTransition("NEXT_STATE", mockEntityProjection());
    }

    @Test(expected = BusinessException.class)
    public void testFailTransitionIfNoNextStatesAssertChangeState() {
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(emptyList());
        xmEntityService.assertStateTransition("NEXT_STATE",  mockEntityProjection());
    }

    @Test(expected = BusinessException.class)
    public void testFailTransitionIfNoMathesStates() {
        List<StateSpec> states = asList(new StateSpec().key("NEXT_STATE_BUT_OTHER"), new StateSpec().key("ORHTER_STATE"));
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(states);
        xmEntityService.assertStateTransition("NEXT_STATE",  mockEntityProjection());
    }

    @Test
    public void testAssertNextState() {
        List<StateSpec> states = asList(new StateSpec().key("FIRST_STATE"), new StateSpec().key("NEXT_STATE"), new StateSpec().key("ORHTER_STATE"));
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(states);
        xmEntityService.assertStateTransition("NEXT_STATE",  mockEntityProjection());
    }

    private XmEntityStateProjection mockEntityProjection() {
        return new XmEntityStateProjection() {
            @Override
            public Long getId() {
                return null;
            }

            @Override
            public String getKey() {
                return null;
            }

            @Override
            public String getTypeKey() {
                return "TEST_TYPE_KEY";
            }

            @Override
            public String getStateKey() {
                return "CURRENT_STATE";
            }
        };
    }

    @Test
    @SneakyThrows
    public void testEntityDataFieldDeserialize() {
        String json = "{\n" + "   \"typeKey\":\"SERVICE-TO-COMPANY\",\n" +
                      "   \"startDate\":\"2019-03-28T12:20:00.000Z\",\n" + "   \"endDate\":null,\n" +
                      "   \"target\":{\n" + "      \"id\":756756756756,\n" +
                      "      \"key\":\"67bb1e21-b72f-4b9c-08f7-43f8767b4974\",\n" + "      \"typeKey\":\"SERVICE\",\n" +
                      "      \"name\":\"Service test\",\n" + "      \"startDate\":\"2019-03-14T09:15:48.429Z\",\n" +
                      "      \"updateDate\":\"2019-03-14T09:15:48.429Z\",\n" + "      \"data\":{\n" +
                      "         \"a\": \"b\"         \n" + "      },\n" +
                      "      \"description\":\"ТЕСТИМ СОЗДНИЕ СЕРВИСА\",\n" + "      \"removed\":false,\n" +
                      "      \"createdBy\":\"kmelnik\",\n" + "      \"version\":0,\n" + "      \"endDate\":null\n" +
                      "   },\n" + "   \"source\":{\n" + "      \"id\":456456456456456,\n" +
                      "      \"key\":\"4e0ab82e-f815-54e9-53ff-04a1be6e7f6a\",\n" + "      \"typeKey\":\"COMPANY\",\n" +
                      "      \"stateKey\":\"ACTIVE\",\n" + "      \"name\":\"123\",\n" +
                      "      \"startDate\":\"2019-03-27T11:35:34.246Z\",\n" +
                      "      \"updateDate\":\"2019-03-28T14:20:30.580Z\",\n" + "      \"data\":{\n" +
                      "         \"a\": \"b\"\n" + "      },\n" + "      \"removed\":false,\n" +
                      "      \"createdBy\":\"kmelnik\",\n" + "      \"version\":4,\n" + "      \"endDate\":null\n" +
                      "   }\n" + "}";

        log.info(json);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.readValue("{}", FunctionContext.class);
        objectMapper.readValue("{}", Comment.class);
        objectMapper.readValue(json, Link.class);
    }
}
