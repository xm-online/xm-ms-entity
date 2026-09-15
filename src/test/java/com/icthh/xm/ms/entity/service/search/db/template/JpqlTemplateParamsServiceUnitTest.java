package com.icthh.xm.ms.entity.service.search.db.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JpqlTemplateParamsServiceUnitTest extends AbstractJupiterUnitTest {

    private JpqlTemplateParamsService service;

    @BeforeEach
    public void setUp() {
        service = new JpqlTemplateParamsService(mock(XmEntityJpqlTemplatesService.class),
            mock(XmAuthenticationContextHolder.class), mock(TenantContextHolder.class));
    }

    @Test
    public void appliesDeclaredParamTypesAndKeepsOthers() {
        JpqlTemplate t = new JpqlTemplate();
        t.setParams(Map.of("n", "number", "f", "number", "b", "boolean", "d", "instant", "l", "list"));

        Map<String, Object> typed = service.applyParamTypes(t, Map.of(
            "n", "5", "f", "2.5", "b", "true", "d", "2026-01-01T00:00:00Z", "l", "a,b", "s", "text", "already", 7));

        assertThat(typed).containsEntry("n", 5L).containsEntry("f", 2.5d).containsEntry("b", true)
            .containsEntry("d", Instant.parse("2026-01-01T00:00:00Z")).containsEntry("l", List.of("a", "b"))
            .containsEntry("s", "text").containsEntry("already", 7);
    }

    @Test
    public void applyParamTypesRejectsBadNumberAndBoolean() {
        JpqlTemplate t = new JpqlTemplate();
        t.setParams(Map.of("n", "number", "b", "boolean"));
        assertThatThrownBy(() -> service.applyParamTypes(t, Map.of("n", "abc")))
            .isInstanceOf(BusinessException.class).hasMessageContaining("n");
        for (String invalid : List.of("yes", "1", "")) {
            assertThatThrownBy(() -> service.applyParamTypes(t, Map.of("b", invalid)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("b");
        }
        assertThat(service.applyParamTypes(t, Map.of("b", "TRUE"))).containsEntry("b", true);
    }
}
