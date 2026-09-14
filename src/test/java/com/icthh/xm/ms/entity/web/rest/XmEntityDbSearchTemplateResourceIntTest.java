package com.icthh.xm.ms.entity.web.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.exceptions.EntityNotFoundException;
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
import org.springframework.util.LinkedMultiValueMap;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchTemplateResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private XmEntityJpqlTemplatesService templatesService;

    private XmEntity active1;
    private XmEntity active2;

    @SneakyThrows
    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        String yml = IOUtils.toString(new ClassPathResource("config/templates/jpql-templates-dbsearch.yml").getInputStream(), UTF_8);
        String key = XmEntityJpqlTemplatesService.TEMPLATES_PATH_PATTERN.replace("{tenantName}", TENANT);
        templatesService.onRefresh(key, yml);
        templatesService.refreshFinished(List.of(key));

        active1 = repository.save(newEntity("ORDER", "B order", Map.of("orderNo", 1)).stateKey("ACTIVE"));
        active2 = repository.save(newEntity("ORDER", "A order", Map.of("orderNo", 1)).stateKey("ACTIVE"));
        repository.save(newEntity("ORDER", "closed", Map.of("orderNo", 1)).stateKey("CLOSED"));
        repository.save(newEntity("ORDER", "other no", Map.of("orderNo", 2)).stateKey("ACTIVE"));
    }

    @SuppressWarnings("unchecked")
    private List<XmEntityDto> entities(Object body) {
        return (List<XmEntityDto>) body;
    }

    @Test
    public void postEntityTemplateBindsTypedParamsAndSorts() {
        var response = resource.searchByTemplatePost("ACTIVE_BY_ORDER",
            Map.of("typeKey", "ORDER", "stateKey", "ACTIVE", "orderNo", 1),
            PageRequest.of(0, 10, Sort.by("name")));

        assertThat(entities(response.getBody())).extracting(XmEntityDto::getId).containsExactly(active2.getId(), active1.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
    }

    @Test
    public void getEntityTemplateCoercesStringParams() {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("typeKey", "ORDER");
        params.add("stateKey", "ACTIVE");
        params.add("orderNo", "1");
        params.add("page", "0");
        params.add("size", "1");

        var response = resource.searchByTemplateGet("ACTIVE_BY_ORDER", params, PageRequest.of(0, 1, Sort.by("name")));

        assertThat(entities(response.getBody())).extracting(XmEntityDto::getId).containsExactly(active2.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("2");
    }

    @Test
    public void missingParamIsRejected() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ACTIVE_BY_ORDER", Map.of("typeKey", "ORDER"), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("stateKey");
    }

    @Test
    public void unknownTemplateIs404() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("NOPE", Map.of(), PageRequest.of(0, 10)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    public void badSortIsRejected() {
        assertThatThrownBy(() -> resource.searchByTemplatePost("ACTIVE_BY_ORDER",
            Map.of("typeKey", "ORDER", "stateKey", "ACTIVE", "orderNo", 1), PageRequest.of(0, 10, Sort.by("evil"))))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(username = "u1", authorities = "SUPER-ADMIN")
    public void subjectParamsOverwriteTheSameNamesSentByClient() {
        XmEntity mine = repository.save(newEntity("ORDER", "mine", Map.of()).createdBy("u1"));
        repository.save(newEntity("ORDER", "victim", Map.of()).createdBy("victim"));

        var post = resource.searchByTemplatePost("MY_ORDERS", Map.of("subjectLogin", "victim"), PageRequest.of(0, 10));
        assertThat(entities(post.getBody())).extracting(XmEntityDto::getId).containsExactly(mine.getId());

        var params = new LinkedMultiValueMap<String, String>();
        params.add("subjectLogin", "victim");
        var get = resource.searchByTemplateGet("MY_ORDERS", params, PageRequest.of(0, 10));
        assertThat(entities(get.getBody())).extracting(XmEntityDto::getId).containsExactly(mine.getId());
    }

    @Test
    @WithMockUser(username = "u1", authorities = "SUPER-ADMIN")
    public void subjectLoginIsBoundAutomatically() {
        XmEntity mine = repository.save(newEntity("ORDER", "mine", Map.of()).createdBy("u1"));
        repository.save(newEntity("ORDER", "not mine", Map.of()).createdBy("somebody-else"));

        var response = resource.searchByTemplatePost("MY_ORDERS", Map.of(), PageRequest.of(0, 10));

        assertThat(entities(response.getBody())).extracting(XmEntityDto::getId).containsExactly(mine.getId());
    }
}
