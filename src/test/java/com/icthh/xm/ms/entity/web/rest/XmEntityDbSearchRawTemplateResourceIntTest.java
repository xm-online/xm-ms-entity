package com.icthh.xm.ms.entity.web.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchRawTemplateResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private XmEntityJpqlTemplatesService templatesService;

    private XmEntity a;
    private XmEntity b;

    @SneakyThrows
    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        String yml = IOUtils.toString(new ClassPathResource("config/templates/jpql-templates-dbsearch.yml").getInputStream(), UTF_8);
        templatesService.onRefresh(applicationProperties.getJpqlTemplatesPathPattern().replace("{tenantName}", TENANT), yml);
        a = repository.save(newEntity("SILENT", "A", Map.of("orderNo", 1)));
        b = repository.save(newEntity("SILENT", "B", Map.of("orderNo", 2)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Object body) {
        return (List<Map<String, Object>>) body;
    }

    @Test
    public void scalarProjectionUsesAliasesAndCountHeaders() {
        var response = resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"), PageRequest.of(0, 1));

        List<Map<String, Object>> rows = rows(response.getBody());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("id", a.getId()).containsEntry("name", "A").containsKey("orderNo");
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
        assertThat(response.getHeaders().getFirst("Link")).contains("rel=\"next\"");
    }

    @Test
    public void entitySelectionIsMappedToDtoAndNoCountHeaderWithoutCountQuery() {
        var response = resource.searchByTemplatePost("ORDER_ENTITIES_RAW", Map.of("typeKey", "SILENT"), PageRequest.of(0, 10));

        List<Map<String, Object>> rows = rows(response.getBody());
        assertThat(rows).hasSize(2);
        Object first = rows.get(0).values().iterator().next();
        assertThat(first).isInstanceOf(XmEntityDto.class);
        assertThat(((XmEntityDto) first).getId()).isEqualTo(a.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isNull();
    }

    @Test
    public void mixedSelectionUsesPositionalKeys() {
        var rows = rows(resource.searchByTemplatePost("MIXED_RAW", Map.of("typeKey", "SILENT"), PageRequest.of(0, 10)).getBody());
        assertThat(rows.get(1).get("col0")).isInstanceOf(XmEntityDto.class);
        assertThat(rows.get(1).get("col1")).isEqualTo("B");
    }

    @Test
    public void sortParamIsRejectedForRaw() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"), PageRequest.of(0, 10, Sort.by("name"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("sort");
    }
}
