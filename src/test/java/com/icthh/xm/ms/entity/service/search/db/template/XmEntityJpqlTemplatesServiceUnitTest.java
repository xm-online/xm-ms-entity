package com.icthh.xm.ms.entity.service.search.db.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.ms.entity.AbstractJupiterUnitTest;
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

}
