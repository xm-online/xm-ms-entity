package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchResourceIntTest extends AbstractPostgresIntTest {

    @Autowired
    private XmEntityDbSearchResource resource;
    @Autowired
    private XmEntityRepository repository;

    private XmEntity kyiv;
    private XmEntity lviv;
    private XmEntity express;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        kyiv = repository.save(newEntity("ORDER", "Alpha", Map.of("orderNo", 1, "customer", Map.of("city", "Kyiv"), "position", 5)));
        lviv = repository.save(newEntity("ORDER", "Beta", Map.of("orderNo", 2, "customer", Map.of("city", "Lviv"), "position", 7)));
        express = repository.save(newEntity("ORDER.EXPRESS", "Gamma", Map.of("orderNo", 3)));
        repository.save(newEntity("PRODUCT", "Kyiv product", Map.of()));
        XmEntity removed = newEntity("ORDER", "Removed Kyiv", Map.of("orderNo", 9));
        removed.setRemoved(true);
        repository.save(removed);
    }

    private static List<Long> ids(ResponseEntity<List<XmEntityDto>> response) {
        return response.getBody().stream().map(XmEntityDto::getId).toList();
    }

    private static XmEntityDbSearchRequest request(String typeKey, String query, Map<String, Object> filter) {
        XmEntityDbSearchRequest r = new XmEntityDbSearchRequest();
        r.setTypeKey(typeKey);
        r.setQuery(query);
        r.setFilter(filter);
        return r;
    }

    @Test
    public void postSearchByTypeKeyIncludesSubTypesAndExcludesRemoved() {
        var response = resource.searchPost(request("ORDER", null, Map.of()), PageRequest.of(0, 10, Sort.by("name")));

        assertThat(ids(response)).containsExactly(kyiv.getId(), lviv.getId(), express.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("3");
        assertThat(response.getHeaders().containsHeader("Link")).isFalse();
    }

    @Test
    public void postExactTypeKeyWhenIncludeSubTypesFalse() {
        XmEntityDbSearchRequest r = request("ORDER", null, Map.of());
        r.setIncludeSubTypes(false);
        assertThat(ids(resource.searchPost(r, PageRequest.of(0, 10)))).containsExactlyInAnyOrder(kyiv.getId(), lviv.getId());
    }

    @Test
    public void fullTextQueryMatchesNameAndDataFieldsCaseInsensitive() {
        assertThat(ids(resource.searchPost(request("ORDER", "kyi", Map.of()), PageRequest.of(0, 10)))).containsExactly(kyiv.getId());
        assertThat(ids(resource.searchPost(request("ORDER", "ALPHA", Map.of()), PageRequest.of(0, 10)))).containsExactly(kyiv.getId());
        // PRODUCT has fullTextSearch: false → search_text null → no match even though name contains Kyiv
        assertThat(ids(resource.searchPost(request("PRODUCT", "kyiv", Map.of()), PageRequest.of(0, 10)))).isEmpty();
    }

    @Test
    public void postFilterOnJsonbAndSortByJsonbDesc() {
        var response = resource.searchPost(
            request("ORDER", null, Map.of("data.orderNo.in", List.of(1, 2, 3), "data.position.gte", 5)),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "data.position")));
        assertThat(ids(response)).containsExactly(lviv.getId(), kyiv.getId());
    }

    @Test
    public void removedIncludedWhenAskedExplicitly() {
        var response = resource.searchPost(request("ORDER", null, Map.of("removed.eq", true)), PageRequest.of(0, 10));
        assertThat(response.getBody()).extracting(XmEntityDto::getName).containsExactly("Removed Kyiv");
    }

    @Test
    public void getVariantBindsFlatQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("typeKey", "ORDER");
        params.add("data.customer.city.eq", "Lviv");
        params.add("page", "0");
        params.add("size", "10");

        var response = resource.searchGet("ORDER", null, null, params, PageRequest.of(0, 10));

        assertThat(ids(response)).containsExactly(lviv.getId());
    }

    @Test
    public void getVariantInfersNumericListValues() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("data.orderNo.in", "1,3");

        var response = resource.searchGet("ORDER", null, true, params, PageRequest.of(0, 10, Sort.by("data.orderNo")));

        assertThat(ids(response)).containsExactly(kyiv.getId(), express.getId());
    }

    @Test
    public void rejectsBadFilterAndSort() {
        assertThatThrownBy(() -> resource.searchPost(request("ORDER", null, Map.of("name.like", "a")), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resource.searchPost(request("ORDER", null, Map.of()), PageRequest.of(0, 10, Sort.by("hacker"))))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> resource.searchPost(request(null, null, Map.of()), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("typeKey");
    }
}
