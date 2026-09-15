package com.icthh.xm.ms.entity.web.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.Tag;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.template.XmEntityJpqlTemplatesService;
import jakarta.persistence.EntityManager;
import java.time.Instant;
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
    @Autowired private EntityManager em;

    private XmEntity a;
    private XmEntity b;

    @SneakyThrows
    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        String yml = IOUtils.toString(new ClassPathResource("config/templates/jpql-templates-dbsearch.yml").getInputStream(), UTF_8);
        String key = XmEntityJpqlTemplatesService.TEMPLATES_PATH_PATTERN.replace("{tenantName}", TENANT);
        templatesService.onRefresh(key, yml);
        templatesService.refreshFinished(List.of(key));
        a = repository.save(newEntity("SILENT", "A", Map.of("orderNo", 1)));
        b = repository.save(newEntity("SILENT", "B", Map.of("orderNo", 2)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Object body) {
        return (List<Map<String, Object>>) body;
    }

    @Test
    public void scalarProjectionUsesAliasesAndCountHeaders() {
        var response = resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"), null, PageRequest.of(0, 1));

        List<Map<String, Object>> rows = rows(response.getBody());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("id", a.getId()).containsEntry("name", "A").containsKey("orderNo");
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
    }

    @Test
    public void skipTotalCountSkipsTheCountQueryOfTheTemplate() {
        var response = resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"), true, PageRequest.of(0, 1));

        assertThat(rows(response.getBody())).hasSize(1);
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isNull();
    }

    @Test
    public void entitySelectionIsMappedToDtoAndNoCountHeaderWithoutCountQuery() {
        var response = resource.searchByTemplatePost("ORDER_ENTITIES_RAW", Map.of("typeKey", "SILENT"), null, PageRequest.of(0, 10));

        List<Map<String, Object>> rows = rows(response.getBody());
        assertThat(rows).hasSize(2);
        Object first = rows.get(0).values().iterator().next();
        assertThat(first).isInstanceOf(XmEntityDto.class);
        assertThat(((XmEntityDto) first).getId()).isEqualTo(a.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isNull();
    }

    @Test
    public void mixedSelectionUsesPositionalKeys() {
        var rows = rows(resource.searchByTemplatePost("MIXED_RAW", Map.of("typeKey", "SILENT"), null, PageRequest.of(0, 10)).getBody());
        assertThat(rows.get(1).get("col0")).isInstanceOf(XmEntityDto.class);
        assertThat(rows.get(1).get("col1")).isEqualTo("B");
    }

    @Test
    public void scalarOnlyProjectionReturnsPlainValues() {
        var rows = rows(resource.searchByTemplatePost("IDS_RAW", Map.of("typeKey", "SILENT"), null, PageRequest.of(0, 10)).getBody());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("col0")).isEqualTo(a.getId());
        assertThat(rows.get(1).get("col0")).isEqualTo(b.getId());
    }

    @Test
    public void entityWithoutDtoMapperIsReturnedAsIs() {
        XmEntity order = repository.save(newEntity("ORDER", "With tag", Map.of()));
        Tag tag = new Tag();
        tag.setTypeKey("VIP");
        tag.setName("vip");
        tag.setStartDate(Instant.now());
        tag.setXmEntity(order);
        em.persist(tag);
        em.flush();

        var rows = rows(resource.searchByTemplatePost("TAGS_RAW", Map.of("tagTypeKey", "VIP"), null, PageRequest.of(0, 10)).getBody());

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("col0")).isInstanceOf(Tag.class);
        assertThat(((Tag) rows.get(0).get("col0")).getId()).isEqualTo(tag.getId());
    }

    @Test
    public void sortFromRequestOverridesTheOrderByOfTheTemplate() {
        // the template itself orders by e.name asc
        var byName = rows(resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"),
            null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"))).getBody());
        assertThat(byName).extracting(row -> row.get("name")).containsExactly("B", "A");

        var ascending = rows(resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"),
            null, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"))).getBody());
        assertThat(ascending).extracting(row -> row.get("name")).containsExactly("A", "B");
    }

    @Test
    public void sortsByAJsonSelectionAliasAndPagesTheSortedResult() {
        var descending = rows(resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"),
            null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "orderNo"))).getBody());
        assertThat(descending).extracting(row -> row.get("name")).containsExactly("B", "A");

        var firstPage = rows(resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"),
            null, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "orderNo"))).getBody());
        assertThat(firstPage).extracting(row -> row.get("name")).containsExactly("B");
    }

    @Test
    public void unknownSortPropertyIsRejectedAndNamesTheAliases() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ORDERS_SUMMARY", Map.of("typeKey", "SILENT"),
            null, PageRequest.of(0, 10, Sort.by("nope"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("nope").hasMessageContaining("name");
    }

    @Test
    public void positionalRowKeysAreNotSortable() {
        // MIXED_RAW selects without aliases, so col0 / col1 are positions, not something to order by
        assertThatThrownBy(() -> resource.searchByTemplatePost("MIXED_RAW", Map.of("typeKey", "SILENT"),
            null, PageRequest.of(0, 10, Sort.by("col0"))))
            .isInstanceOf(BusinessException.class).hasMessageContaining("col0");
    }
}
