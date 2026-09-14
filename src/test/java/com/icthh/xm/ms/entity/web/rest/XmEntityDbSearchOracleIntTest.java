package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractOracleIntTest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Oracle behaves differently from PostgreSQL for json: values come out as scalars, so both comparison and
 * ordering need an explicit SQL type. Without it Oracle compares numbers as text and ranks 10 before 2.
 */
@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchOracleIntTest extends AbstractOracleIntTest {

    private static final String TYPE = "ORDER";

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        repository.save(newEntity(TYPE, "one", Map.of("orderNo", 1, "customer", Map.of("city", "Kyiv"))));
        repository.save(newEntity(TYPE, "two", Map.of("orderNo", 2, "customer", Map.of("city", "Lviv"))));
        repository.save(newEntity(TYPE, "ten", Map.of("orderNo", 10, "customer", Map.of("city", "Kharkiv"))));
    }

    private static XmEntityDbSearchRequest request(Map<String, Object> filter) {
        XmEntityDbSearchRequest r = new XmEntityDbSearchRequest();
        r.setTypeKey(TYPE);
        r.setFilter(filter);
        return r;
    }

    private List<String> names(Map<String, Object> filter, Sort sort) {
        return resource.searchPost(request(filter), PageRequest.of(0, 10, sort))
            .getBody().stream().map(XmEntityDto::getName).toList();
    }

    @Test
    public void numericOrderingOfDataFieldIsNumericNotLexical() {
        // lexical ordering would give one, ten, two
        assertThat(names(Map.of(), Sort.by(Sort.Direction.ASC, "data.orderNo")))
            .containsExactly("one", "two", "ten");
        assertThat(names(Map.of(), Sort.by(Sort.Direction.DESC, "data.orderNo")))
            .containsExactly("ten", "two", "one");
    }

    @Test
    public void textOrderingOfDataFieldStillWorks() {
        assertThat(names(Map.of(), Sort.by(Sort.Direction.ASC, "data.customer.city")))
            .containsExactly("ten", "one", "two");
    }

    @Test
    public void numericRangeFiltersCompareAsNumbers() {
        assertThat(names(Map.of("data.orderNo.gt", 2), Sort.unsorted())).containsExactly("ten");
        assertThat(names(Map.of("data.orderNo.lte", 2), Sort.by("name"))).containsExactly("one", "two");
        assertThat(names(Map.of("data.orderNo.gte", 10), Sort.unsorted())).containsExactly("ten");
    }

    @Test
    public void equalityAndInOnNumericDataField() {
        assertThat(names(Map.of("data.orderNo.eq", 10), Sort.unsorted())).containsExactly("ten");
        assertThat(names(Map.of("data.orderNo.in", List.of(1, 10)), Sort.by("name"))).containsExactly("one", "ten");
    }

    @Test
    public void stringDataFieldEqualityAndContains() {
        assertThat(names(Map.of("data.customer.city.eq", "Kyiv"), Sort.unsorted())).containsExactly("one");
        assertThat(names(Map.of("data.customer.city.contains", "HARK"), Sort.unsorted())).containsExactly("ten");
    }

    @Test
    public void hasOperatorIsRejectedOnOracle() {
        assertThatThrownBy(() -> names(Map.of("data.tags.has", "claude"), Sort.unsorted()))
            .isInstanceOf(BusinessException.class).hasMessageContaining("not supported on Oracle");
    }

    @Test
    public void fullTextQueryOverSearchText() {
        XmEntityDbSearchRequest request = request(Map.of());
        request.setQuery("kyi");

        var response = resource.searchPost(request, PageRequest.of(0, 10));

        assertThat(response.getBody()).extracting(XmEntityDto::getName).containsExactly("one");
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("1");
    }

    @Test
    public void columnFilterAndSortStillWork() {
        // only one and ten contain "e"
        assertThat(names(Map.of("name.contains", "e"), Sort.by(Sort.Direction.DESC, "name")))
            .containsExactly("ten", "one");
    }
}
