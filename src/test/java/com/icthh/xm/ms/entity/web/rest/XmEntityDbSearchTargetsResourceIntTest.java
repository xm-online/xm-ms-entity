package com.icthh.xm.ms.entity.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.Link;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.LinkRepository;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.service.dto.LinkDto;
import com.icthh.xm.ms.entity.service.search.db.dto.XmEntityDbSearchRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@WithMockUser(authorities = "SUPER-ADMIN")
public class XmEntityDbSearchTargetsResourceIntTest extends AbstractPostgresIntTest {

    @Autowired private XmEntityDbSearchResource resource;
    @Autowired private XmEntityRepository repository;
    @Autowired private LinkRepository linkRepository;

    private XmEntity order;
    private Link toA;
    private Link toB;

    private Link link(XmEntity source, XmEntity target, String typeKey) {
        Link link = new Link();
        link.setTypeKey(typeKey);
        link.setSource(source);
        link.setTarget(target);
        link.setStartDate(Instant.now());
        return linkRepository.save(link);
    }

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        order = repository.save(newEntity("ORDER", "Order", Map.of()));
        XmEntity a = repository.save(newEntity("PRODUCT", "Product A", Map.of("sku", "A1", "price", 10)));
        XmEntity b = repository.save(newEntity("PRODUCT", "Product B", Map.of("sku", "B2", "price", 20)));
        toA = link(order, a, "ORDER.ITEM");
        toB = link(order, b, "ORDER.ITEM");
        link(order, a, "ORDER.NOTE");
        XmEntity otherOrder = repository.save(newEntity("ORDER", "Other", Map.of()));
        link(otherOrder, a, "ORDER.ITEM");
    }

    private List<Long> ids(XmEntityDbSearchRequest request, Sort sort) {
        return resource.searchTargetsPost(order.getId().toString(), "ORDER.ITEM", request, PageRequest.of(0, 10, sort))
            .getBody().stream().map(LinkDto::getId).toList();
    }

    @Test
    public void returnsLinksOfSourceAndTypeOnly() {
        assertThat(ids(new XmEntityDbSearchRequest(), Sort.unsorted())).containsExactlyInAnyOrder(toA.getId(), toB.getId());
    }

    @Test
    public void filtersAndSortsByTargetFields() {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setFilter(Map.of("data.price.gt", 15));
        assertThat(ids(request, Sort.unsorted())).containsExactly(toB.getId());

        XmEntityDbSearchRequest byName = new XmEntityDbSearchRequest();
        byName.setFilter(Map.of("name.contains", "product"));
        assertThat(ids(byName, Sort.by(Sort.Direction.DESC, "data.price"))).containsExactly(toB.getId(), toA.getId());
    }

    @Test
    public void unknownLinkTypeIsRejected() {
        assertThatThrownBy(() -> resource.searchTargetsPost(order.getId().toString(), "NOPE",
            new XmEntityDbSearchRequest(), PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("NOPE");

        var params = new org.springframework.util.LinkedMultiValueMap<String, String>();
        assertThatThrownBy(() -> resource.searchTargetsGet(order.getId().toString(), "NOPE", null, null, null,
            params, PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("NOPE");
    }

    @Test
    public void unknownTargetTypeKeyIsRejected() {
        XmEntityDbSearchRequest request = new XmEntityDbSearchRequest();
        request.setTypeKey("NO_SUCH_TYPE");
        assertThatThrownBy(() -> resource.searchTargetsPost(order.getId().toString(), "ORDER.ITEM", request,
            PageRequest.of(0, 10)))
            .isInstanceOf(BusinessException.class).hasMessageContaining("NO_SUCH_TYPE");
    }

    @Test
    public void bodyContainsTargetDto() {
        var body = resource.searchTargetsPost(order.getId().toString(), "ORDER.ITEM", new XmEntityDbSearchRequest(), PageRequest.of(0, 10)).getBody();
        assertThat(body).isNotEmpty().allSatisfy(link -> assertThat(link.getTarget()).isNotNull());
    }
}
