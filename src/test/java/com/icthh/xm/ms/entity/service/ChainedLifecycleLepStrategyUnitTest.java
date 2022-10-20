package com.icthh.xm.ms.entity.service;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.lep.keyresolver.TypeKeyWithExtends;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.ReflectionUtils;

import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChainedLifecycleLepStrategyUnitTest extends AbstractUnitTest {

    private final long DEF_ID = 0L;

    ChainedLifecycleLepStrategy service;

    @Mock
    XmEntityLifeCycleService lifeCycleService;
    @Mock
    TypeKeyWithExtends typeKeyWithExtends;

    @Before
    public void setUp() {
        service = new ChainedLifecycleLepStrategy(lifeCycleService, typeKeyWithExtends);
        ReflectionUtils.makeAccessible(Objects.requireNonNull(
            ReflectionUtils.findField(ChainedLifecycleLepStrategy.class, "internal")));
        ReflectionUtils.setField(Objects.requireNonNull(
            ReflectionUtils.findField(ChainedLifecycleLepStrategy.class, "internal")), service, service);
    }

    @Test
    public void testChangeStateByTransitionWithInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO-INHERIT";
        String entTypeKeyChild = "DEMO-INHERIT-CHILD";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.TRUE);
        when(typeKeyWithExtends.nextTypeKey(eq(entTypeKey))).thenReturn(entTypeKeyChild);

        when(service.changeStateByXmEntity(idOrKey, entTypeKeyChild, "NEW", Map.of(), entTypeKey))
            .thenReturn(newResultEntity(entTypeKeyChild));

        XmEntity res = service.changeStateByTransition(idOrKey, entTypeKey, "OLD", "NEW", Map.of());

        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKeyChild, res.getTypeKey());
    }

    @Test
    public void testChangeStateByTransitionWithoutInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.FALSE);

        when(lifeCycleService.changeState(idOrKey, "NEW", Map.of()))
            .thenReturn(newResultEntity(entTypeKey));

        XmEntity res = service.changeStateByTransition(idOrKey, entTypeKey, "OLD", "NEW", Map.of());
        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKey, res.getTypeKey());
    }

    @Test
    public void testChangeStateByTargetStateWithInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO-INHERIT";
        String entTypeKeyChild = "DEMO-INHERIT-CHILD";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.TRUE);
        when(typeKeyWithExtends.nextTypeKey(eq(entTypeKey))).thenReturn(entTypeKeyChild);

        when(service.changeStateByXmEntity(idOrKey, entTypeKeyChild, "NEW", Map.of(), entTypeKey))
            .thenReturn(newResultEntity(entTypeKeyChild));

        XmEntity res = service.changeStateByTargetState(idOrKey, entTypeKey, "NEW", Map.of());

        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKeyChild, res.getTypeKey());
    }

    @Test
    public void testChangeStateByTargetStateWithoutInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.FALSE);

        when(lifeCycleService.changeState(idOrKey, "NEW", Map.of()))
            .thenReturn(newResultEntity(entTypeKey));

        XmEntity res = service.changeStateByTargetState(idOrKey, entTypeKey, "NEW", Map.of());
        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKey, res.getTypeKey());
    }

    @Test
    public void testChangeStateByXmEntityWithInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO-INHERIT";
        String entTypeKeyChild = "DEMO-INHERIT-CHILD";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.TRUE);
        when(typeKeyWithExtends.nextTypeKey(eq(entTypeKey))).thenReturn(entTypeKeyChild);

        when(service.changeStateByXmEntity(idOrKey, entTypeKeyChild, "NEW", Map.of(), entTypeKey))
            .thenReturn(newResultEntity(entTypeKeyChild));

        XmEntity res = service.changeStateByXmEntity(idOrKey, entTypeKey, "NEW", Map.of());

        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKeyChild, res.getTypeKey());
    }

    @Test
    public void testChangeStateByXmEntityWithoutInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.FALSE);

        when(lifeCycleService.changeState(idOrKey, "NEW", Map.of()))
            .thenReturn(newResultEntity(entTypeKey));

        XmEntity res = service.changeStateByXmEntity(idOrKey, entTypeKey, "NEW", Map.of());
        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKey, res.getTypeKey());
    }

    @Test
    public void changeStateWithInheritance() {
        IdOrKey idOrKey = IdOrKey.of(0L);
        String entTypeKey = "DEMO-INHERIT";
        String entTypeKeyChild = "DEMO-INHERIT-CHILD";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.TRUE);
        when(typeKeyWithExtends.nextTypeKey(eq(entTypeKey))).thenReturn(entTypeKeyChild);

        when(service.changeStateByTargetState(idOrKey, entTypeKeyChild, "NEW", Map.of(), entTypeKey))
            .thenReturn(newResultEntity(entTypeKeyChild));
        XmEntity res = service.changeState(idOrKey, entTypeKey, "OLD", "NEW", Map.of());

        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKeyChild, res.getTypeKey());
    }

    @Test
    public void changeStateWithoutInheritance() {
        IdOrKey idOrKey = IdOrKey.of(DEF_ID);
        String entTypeKey = "DEMO-NO-INHERIT";
        when(typeKeyWithExtends.doInheritance(eq(entTypeKey))).thenReturn(Boolean.FALSE);

        when(service.changeStateByTargetState(idOrKey, entTypeKey+"-CHILD", "NEW", Map.of()))
            .thenReturn(newResultEntity(entTypeKey));
        XmEntity res = service.changeState(idOrKey, entTypeKey, "OLD", "NEW", Map.of());

        assertEquals(idOrKey.getId(), res.getId());
        assertEquals(entTypeKey, res.getTypeKey());
    }

    private XmEntity newResultEntity(String typeKey) {
        XmEntity xmEntity = new XmEntity();
        xmEntity.setId(DEF_ID);
        xmEntity.setTypeKey(typeKey);
        return xmEntity;
    }

}
