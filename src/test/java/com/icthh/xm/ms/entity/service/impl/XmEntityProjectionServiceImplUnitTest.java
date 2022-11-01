package com.icthh.xm.ms.entity.service.impl;

import com.icthh.xm.ms.entity.AbstractUnitTest;
import com.icthh.xm.ms.entity.domain.ext.IdOrKey;
import com.icthh.xm.ms.entity.projection.XmEntityIdKeyTypeKey;
import com.icthh.xm.ms.entity.projection.XmEntityStateProjection;
import com.icthh.xm.ms.entity.repository.XmEntityProjectionRepository;
import com.icthh.xm.ms.entity.service.XmEntityProjectionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class XmEntityProjectionServiceImplUnitTest extends AbstractUnitTest {

    @Mock
    XmEntityProjectionRepository xmEntityProjectionRepository;

    XmEntityProjectionService xmEntityProjectionService;

    @Before
    public void setUp() throws Exception {
        xmEntityProjectionService = new XmEntityProjectionServiceImpl(xmEntityProjectionRepository);
    }

    @Test
    public void findStateProjectionByIdReturnEmpty() {
        IdOrKey idOrKey = null;
        Optional<XmEntityStateProjection> stateProjection = xmEntityProjectionService.findStateProjection(idOrKey);
        assertThat(stateProjection).isEmpty();
    }

    @Test
    public void findStateProjectionByIdReturnEmptyIfNotFound() {
        IdOrKey idOrKey = IdOrKey.of(100L);
        when(xmEntityProjectionRepository.findStateProjectionById(eq(100L))).thenReturn(null);
        Optional<XmEntityStateProjection> stateProjection = xmEntityProjectionService.findStateProjection(idOrKey);
        assertThat(stateProjection).isEmpty();
    }

    @Test
    public void findStateProjectionByIdReturnValue() {
        Long key = 100L;
        IdOrKey idOrKey = IdOrKey.of(key);
        when(xmEntityProjectionRepository.findStateProjectionById(eq(key))).thenReturn(newProjection(key, key.toString(), "S1"));
        Optional<XmEntityStateProjection> stateProjection = xmEntityProjectionService.findStateProjection(idOrKey);
        assertThat(stateProjection).isNotEmpty();
        assertThat(stateProjection.get().getStateKey()).isEqualTo("S1");
    }

    @Test
    public void findStateProjectionByKeyReturnEmptyIfNotFound() {
        IdOrKey idOrKey = IdOrKey.of("100");
        when(xmEntityProjectionRepository.findStateProjectionByKey(eq("100"))).thenReturn(null);
        Optional<XmEntityStateProjection> stateProjection = xmEntityProjectionService.findStateProjection(idOrKey);
        assertThat(stateProjection).isEmpty();
    }

    @Test
    public void findStateProjectionByKeyReturnValue() {
        String key = "key";
        IdOrKey idOrKey = IdOrKey.of(key);
        when(xmEntityProjectionRepository.findStateProjectionByKey(eq(key))).thenReturn(newProjection(100L, key, "S1"));
        Optional<XmEntityStateProjection> stateProjection = xmEntityProjectionService.findStateProjection(idOrKey);
        assertThat(stateProjection).isNotEmpty();
        assertThat(stateProjection.get().getStateKey()).isEqualTo("S1");
    }

    @Test
    public void findXmEntityIdKeyTypeKeyReturnEmpty() {
        IdOrKey idOrKey = null;
        Optional<XmEntityIdKeyTypeKey> stateProjection = xmEntityProjectionService.findXmEntityIdKeyTypeKey(idOrKey);
        assertThat(stateProjection).isEmpty();
    }

    @Test
    public void findXmEntityIdKeyTypeKeyByIdReturnEmptyIfNotFound() {
        IdOrKey idOrKey = IdOrKey.of(100L);
        when(xmEntityProjectionRepository.findOneIdKeyTypeKeyById(eq(100L))).thenReturn(null);
        Optional<XmEntityIdKeyTypeKey> stateProjection = xmEntityProjectionService.findXmEntityIdKeyTypeKey(idOrKey);
        assertThat(stateProjection).isEmpty();
    }

    @Test
    public void findXmEntityIdKeyTypeKeyByIdReturnValue() {
        Long key = 100L;
        IdOrKey idOrKey = IdOrKey.of(key);
        when(xmEntityProjectionRepository.findOneIdKeyTypeKeyById(eq(key))).thenReturn(newIdTkProjection(key, key.toString()));
        Optional<XmEntityIdKeyTypeKey> stateProjection = xmEntityProjectionService.findXmEntityIdKeyTypeKey(idOrKey);
        assertThat(stateProjection).isNotEmpty();
        assertThat(stateProjection.get().getTypeKey()).isEqualTo("DEMO");
    }

    @Test
    public void findXmEntityIdKeyTypeKeyByKeyReturnEmptyIfNotFound() {
        IdOrKey idOrKey = IdOrKey.of("key");
        when(xmEntityProjectionRepository.findOneIdKeyTypeKeyByKey(eq("key"))).thenReturn(null);
        Optional<XmEntityIdKeyTypeKey> stateProjection = xmEntityProjectionService.findXmEntityIdKeyTypeKey(idOrKey);
        assertThat(stateProjection).isEmpty();
    }

    @Test
    public void findXmEntityIdKeyTypeKeyByKeyReturnValue() {
        String key = "key";
        IdOrKey idOrKey = IdOrKey.of(key);
        when(xmEntityProjectionRepository.findOneIdKeyTypeKeyByKey(eq(key))).thenReturn(newIdTkProjection(100L, key));
        Optional<XmEntityIdKeyTypeKey> stateProjection = xmEntityProjectionService.findXmEntityIdKeyTypeKey(idOrKey);
        assertThat(stateProjection).isNotEmpty();
        assertThat(stateProjection.get().getTypeKey()).isEqualTo("DEMO");
    }

    private XmEntityIdKeyTypeKey newIdTkProjection(Long id, String key) {
        return new XmEntityIdKeyTypeKey() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getTypeKey() {
                return "DEMO";
            }
        };
    }

    private XmEntityStateProjection newProjection(Long id, String key, String state) {
        return new XmEntityStateProjection() {
            @Override
            public String getStateKey() {
                return state;
            }

            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getTypeKey() {
                return "DEMO";
            }
        };
    }

}
