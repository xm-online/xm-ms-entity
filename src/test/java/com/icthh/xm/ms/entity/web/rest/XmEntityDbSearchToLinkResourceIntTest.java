package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.XmEntityDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchToLinkResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private LinkRepository linkRepository;

    private XmEntity order;
    private XmEntity linkedProduct;
    private XmEntity freeProduct;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        order = repository.save(newEntity("ORDER", "Order", Map.of()));
        linkedProduct = repository.save(newEntity("PRODUCT", "Linked product", Map.of("sku", "A1")));
        freeProduct = repository.save(newEntity("PRODUCT", "Free product", Map.of("sku", "B2")));
        repository.save(newEntity("ORDER", "Another order", Map.of()));
        Link link = new Link();
        link.setTypeKey("ORDER.ITEM");
        link.setSource(order);
        link.setTarget(linkedProduct);
        link.setStartDate(Instant.now());
        linkRepository.save(link);
    }

    private List<Long> ids(String linkTypeKey, XmEntityDbSearchRequest request) {
        return resource.searchToLinkPost("ORDER", order.getId().toString(), linkTypeKey, request, PageRequest.of(0, 10))
            .getBody().stream().map(XmEntityDto::getId).toList();
    }

    @Test
    public void uniqueLinkExcludesAlreadyLinkedTargets() {
        assertThat(ids("ORDER.ITEM", new XmEntityDbSearchRequest())).containsExactly(freeProduct.getId());
    }

    @Test
    public void nonUniqueLinkReturnsAllTargetsOfLinkType() {
        assertThat(ids("ORDER.NOTE", new XmEntityDbSearchRequest()))
            .containsExactlyInAnyOrder(linkedProduct.getId(), freeProduct.getId());
    }

    @Test
    public void filterAppliesToCandidates() {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setFilter(Map.of("data.sku.eq", "A1"));
        assertThat(ids("ORDER.NOTE", request)).containsExactly(linkedProduct.getId());
        assertThat(ids("ORDER.ITEM", request)).isEmpty();
    }

    @Test
    public void getVariantAndEntityKeyResolution() {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("data.sku.eq", "B2");
        var response = resource.searchToLinkGet("ORDER", order.getKey(), "ORDER.ITEM", null, null, params, PageRequest.of(0, 10));
        assertThat(response.getBody()).extracting(XmEntityDto::getId).containsExactly(freeProduct.getId());
        assertThat(response.getHeaders().getFirst("X-Total-Count")).isEqualTo("1");
    }

    @Test
    public void unknownLinkTypeIsRejected() {
        assertThatThrownBy(() -> ids("NOPE", new XmEntityDbSearchRequest()))
            .isInstanceOf(BusinessException.class).hasMessageContaining("NOPE");
    }
}
