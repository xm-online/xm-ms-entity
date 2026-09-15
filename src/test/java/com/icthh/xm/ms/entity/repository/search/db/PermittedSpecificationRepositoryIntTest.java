package com.icthh.xm.ms.entity.repository.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelTranslator;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.domain.XmEntity_;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PermittedSpecificationRepositoryIntTest extends AbstractPostgresIntTest {

    @Autowired
    private PermittedSpecificationRepository repository;
    @Autowired
    private XmEntityRepository xmEntityRepository;
    @MockitoBean
    private PermissionCheckService permissionCheckService;

    private static final String TYPE = "SILENT";

    private XmEntity mine;
    private XmEntity other;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        mine = xmEntityRepository.save(newEntity(TYPE, "mine", Map.of("orderNo", 1)).createdBy("user-1"));
        other = xmEntityRepository.save(newEntity(TYPE, "other", Map.of("orderNo", 2)).createdBy("user-2"));
        xmEntityRepository.save(newEntity("PRODUCT", "third", Map.of()).createdBy("user-1"));
    }

    private void permissionCondition(String jpqlWithReturnObjectAlias) {
        Mockito.when(permissionCheckService.createCondition(Mockito.any(), Mockito.eq("PRIV"), Mockito.any(SpelTranslator.class)))
            .thenReturn(jpqlWithReturnObjectAlias);
    }

    private static Specification<XmEntity> typeKey(String typeKey) {
        return (root, query, cb) -> cb.equal(root.get(XmEntity_.typeKey), typeKey);
    }

    @Test
    public void specificationAndPermissionConditionAreBothApplied() {
        permissionCondition("returnObject.createdBy = 'user-1'");

        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey(TYPE), null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactly(mine.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void emptyPermissionConditionAppliesOnlySpecification() {
        permissionCondition("");

        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey(TYPE), null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactlyInAnyOrder(mine.getId(), other.getId());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void ordersAndPagingAreApplied() {
        permissionCondition("");
        OrderProvider<XmEntity> byNameDesc = (root, cb) -> List.of(cb.desc(root.get(XmEntity_.name)));

        Page<XmEntity> first = repository.findAll(XmEntity.class, typeKey(TYPE), byNameDesc, PageRequest.of(0, 1), "PRIV");
        Page<XmEntity> second = repository.findAll(XmEntity.class, typeKey(TYPE), byNameDesc, PageRequest.of(1, 1), "PRIV");

        assertThat(first.getContent()).extracting(XmEntity::getName).containsExactly("other");
        assertThat(second.getContent()).extracting(XmEntity::getName).containsExactly("mine");
        assertThat(first.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void whereFragmentWithNamedParamsIsCombinedWithPermissionAndSpecification() {
        permissionCondition("returnObject.typeKey = '" + TYPE + "'");

        Page<XmEntity> page = repository.findAll(XmEntity.class,
            "entity.name = :name", Map.of("name", "other"),
            null, null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactly(other.getId());
    }

    @Test
    public void aliasIsRewrittenAsWholeWordOnly() {
        XmEntity named = xmEntityRepository.save(newEntity(TYPE, "returnObject", Map.of()).createdBy("user-3"));
        permissionCondition("returnObject.name = 'returnObject'");

        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey(TYPE), null, PageRequest.of(0, 10), "PRIV");

        assertThat(page.getContent()).extracting(XmEntity::getId).containsExactly(named.getId());
    }

    @Test
    public void nullPrivilegeKeySkipsPermissionCondition() {
        Page<XmEntity> page = repository.findAll(XmEntity.class, typeKey(TYPE), null, PageRequest.of(0, 10), null);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
