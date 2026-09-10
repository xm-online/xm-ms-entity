package com.icthh.xm.ms.entity.service.search.db.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class XmEntityJpqlTemplatesServiceUnitTest extends AbstractJupiterUnitTest {

    private static final String FILE_KEY = "/config/tenants/TEST/entity/jpql-templates.yml";
    private static final String FOLDER_KEY_A = "/config/tenants/TEST/entity/jpql-templates/a.yml";

    private XmEntityJpqlTemplatesService service;

    @BeforeEach
    public void setUp() {
        TenantContextHolder holder = mock(TenantContextHolder.class);
        TenantContext ctx = mock(TenantContext.class);
        when(holder.getContext()).thenReturn(ctx);
        when(ctx.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        service = new XmEntityJpqlTemplatesService(holder);
    }

    private void refresh(String key, String yml) {
        service.onRefresh(key, yml);
        service.refreshFinished(List.of(key));
    }

    @Test
    public void listensToFileAndFolderPatternsOnly() {
        assertThat(service.isListeningConfiguration(FILE_KEY)).isTrue();
        assertThat(service.isListeningConfiguration(FOLDER_KEY_A)).isTrue();
        assertThat(service.isListeningConfiguration("/config/tenants/TEST/entity/search-templates.yml")).isFalse();
    }

    @Test
    public void mergesFileAndFolderAndParsesTypes() {
        refresh(FILE_KEY, "A:\n  query: \"entity.name = :name\"\n");
        refresh(FOLDER_KEY_A, "B:\n  type: RAW\n  query: \"select e from XmEntity e where e.id = :id\"\n"
            + "  countQuery: \"select count(e) from XmEntity e where e.id = :id\"\n  params:\n    id: number\n");

        JpqlTemplate a = service.getTemplate("A");
        assertThat(a.getKey()).isEqualTo("A");
        assertThat(a.getType()).isEqualTo(JpqlTemplateType.ENTITY);

        JpqlTemplate b = service.getTemplate("B");
        assertThat(b.getType()).isEqualTo(JpqlTemplateType.RAW);
        assertThat(b.getCountQuery()).contains("count(e)");
        assertThat(b.getParams()).containsEntry("id", "number");
    }

    @Test
    public void templateWithoutQueryIsIgnoredOnRefresh() {
        refresh(FILE_KEY, "EMPTY:\n  type: RAW\n");
        assertThatThrownBy(() -> service.getTemplate("EMPTY")).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void removingConfigDropsTemplates() {
        refresh(FILE_KEY, "A:\n  query: \"entity.name = :name\"\n");
        refresh(FILE_KEY, null);
        assertThatThrownBy(() -> service.getTemplate("A")).isInstanceOf(EntityNotFoundException.class);
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
    public void applyParamTypesRejectsBadNumber() {
        JpqlTemplate t = new JpqlTemplate();
        t.setParams(Map.of("n", "number"));
        assertThatThrownBy(() -> service.applyParamTypes(t, Map.of("n", "abc")))
            .isInstanceOf(BusinessException.class).hasMessageContaining("n");
    }
}
