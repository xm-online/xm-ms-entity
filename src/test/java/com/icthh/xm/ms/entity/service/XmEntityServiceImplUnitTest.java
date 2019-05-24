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
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.UniqueField;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.spec.LinkSpec;
import com.icthh.xm.ms.entity.domain.spec.StateSpec;
import com.icthh.xm.ms.entity.domain.spec.TypeSpec;
import com.icthh.xm.ms.entity.domain.spec.UniqueFieldSpec;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.SpringXmEntityRepository;
import com.icthh.xm.ms.entity.repository.UniqueFieldRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepositoryInternal;
import com.icthh.xm.ms.entity.repository.search.XmEntitySearchRepository;
import com.icthh.xm.ms.entity.service.impl.StartUpdateDateGenerationStrategy;
import com.icthh.xm.ms.entity.service.impl.XmEntityServiceImpl;

import java.time.Instant;
import java.util.*;

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

    private static final Instant MOCKED_START_DATE = Instant.ofEpochMilli(42L);
    private static final Instant MOCKED_UPDATE_DATE = Instant.ofEpochMilli(84L);

    @InjectMocks
    private XmEntityServiceImpl xmEntityService;

    @Mock
    private XmEntitySpecService xmEntitySpecService;
    @Mock
    private LinkRepository linkRepository;
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
    @Mock
    private SpringXmEntityRepository springXmEntityRepository;
    @Mock
    private XmAuthenticationContextHolder authContextHolder;
    @Mock
    private XmAuthenticationContext context;
    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void before() {
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
        xmEntityService.assertStateTransition("NEXT_STATE", mockEntityProjection("TEST_TYPE_KEY"));
    }

    @Test(expected = BusinessException.class)
    public void testFailTransitionIfNoNextStatesAssertChangeState() {
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(emptyList());
        xmEntityService.assertStateTransition("NEXT_STATE", mockEntityProjection("TEST_TYPE_KEY"));
    }

    @Test(expected = BusinessException.class)
    public void testFailTransitionIfNoMathesStates() {
        List<StateSpec> states = asList(new StateSpec().key("NEXT_STATE_BUT_OTHER"), new StateSpec().key("ORHTER_STATE"));
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(states);
        xmEntityService.assertStateTransition("NEXT_STATE", mockEntityProjection("TEST_TYPE_KEY"));
    }

    @Test
    public void testAssertNextState() {
        List<StateSpec> states = asList(new StateSpec().key("FIRST_STATE"), new StateSpec().key("NEXT_STATE"), new StateSpec().key("ORHTER_STATE"));
        when(xmEntitySpecService.nextStates("TEST_TYPE_KEY", "CURRENT_STATE")).thenReturn(states);
        xmEntityService.assertStateTransition("NEXT_STATE", mockEntityProjection("TEST_TYPE_KEY"));
    }

    private XmEntityStateProjection mockEntityProjection(String type) {
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
                return type;
            }

            @Override
            public String getStateKey() {
                return "CURRENT_STATE";
            }
        };
    }
    @Test
    public void assertSourceEntityMaxLinkValue() {

        XmEntity testEntity = new XmEntity().typeKey("XM_TEST_ENTITY");
        XmEntity source1 = new XmEntity().typeKey("XM_SOURCE_ENTITY");
        source1.setId(1L);
        XmEntity source2 = new XmEntity().typeKey("XM_SOURCE_ENTITY");
        source2.setId(2L);

        Link link1 = new Link().typeKey("LINK_TYPE_KEY").source(source1).target(testEntity);
        Link link11 = new Link().typeKey("LINK_TYPE_KEY").source(source1).target(testEntity);
        Link link2 = new Link().typeKey("LINK_TYPE_KEY").source(source2).target(testEntity);
        Link linkOld = new Link().typeKey("LINK_TYPE_KEY").source(source2).target(testEntity);
        linkOld.setId(1L);

        Set<Link> testSources = new HashSet<>();
        testSources.add(link1);
        testSources.add(link11);
        testSources.add(link2);
        testSources.add(linkOld);
        testEntity.setSources(testSources);

        List<Long> sourceIds = new ArrayList<>();
        sourceIds.add(1L);
        sourceIds.add(2L);

        List<XmEntityStateProjection> projectionList = new ArrayList<>();
        projectionList.add(mockEntityProjection("XM_SOURCE_ENTITY"));
        projectionList.add(mockEntityProjection("XM_SOURCE_ENTITY"));

        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));
        when(xmEntityRepository.findAllStateProjectionByIdIn(sourceIds)).thenReturn(projectionList);
        when(linkRepository.countBySourceIdAndTypeKey(source1.getId(), "LINK_TYPE_KEY")).thenReturn(0);
        xmEntityService.save(testEntity);
    }
    private Optional<LinkSpec> createLinkSpeckOptional(int maxValue) {
        LinkSpec linkSpec = new LinkSpec();
        linkSpec.setMax(maxValue);
        return Optional.of(linkSpec);
    }
}
