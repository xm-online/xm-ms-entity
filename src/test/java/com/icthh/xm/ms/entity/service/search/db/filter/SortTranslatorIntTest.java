package com.icthh.xm.ms.entity.service.search.db.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import com.icthh.xm.ms.entity.repository.search.db.PermittedSpecificationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SortTranslatorIntTest extends AbstractPostgresIntTest {

    private static final String TYPE = "SILENT";

    @Autowired
    private SortTranslator sortTranslator;
    @Autowired
    private XmEntityFilterSpecificationBuilder builder;
    @Autowired
    private PermittedSpecificationRepository repository;
    @Autowired
    private XmEntityRepository xmEntityRepository;

    @BeforeEach
    public void seed() {
        pushDbSearchSpec();
        xmEntityRepository.save(newEntity(TYPE, "b", Map.of("orderNo", 10)));
        xmEntityRepository.save(newEntity(TYPE, "a", Map.of("orderNo", 2)));
        xmEntityRepository.save(newEntity(TYPE, "c", Map.of("orderNo", 1)));
    }

    private List<String> namesSortedBy(Sort sort) {
        return repository.findAll(XmEntity.class, builder.typeKey(TYPE, false, root -> root),
                sortTranslator.toOrderProvider(sort), PageRequest.of(0, 10), null)
            .getContent().stream().map(XmEntity::getName).toList();
    }

    @Test
    public void sortsByColumn() {
        assertThat(namesSortedBy(Sort.by(Sort.Direction.ASC, "name"))).containsExactly("a", "b", "c");
        assertThat(namesSortedBy(Sort.by(Sort.Direction.DESC, "name"))).containsExactly("c", "b", "a");
    }

    @Test
    public void sortsByDataPathNumerically() {
        assertThat(namesSortedBy(Sort.by(Sort.Direction.ASC, "data.orderNo"))).containsExactly("c", "a", "b");
        assertThat(namesSortedBy(Sort.by(Sort.Direction.DESC, "data.orderNo"))).containsExactly("b", "a", "c");
    }

    @Test
    public void unsortedGivesNoOrder() {
        assertThat(namesSortedBy(Sort.unsorted())).hasSize(3);
    }

    @Test
    public void rejectsUnknownProperty() {
        assertThatThrownBy(() -> sortTranslator.validate(Sort.by("avatarUrlRelative")))
            .isInstanceOf(BusinessException.class).hasMessageContaining("avatarUrlRelative");
        assertThatThrownBy(() -> sortTranslator.validate(Sort.by("1=1; drop table xm_entity")))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> namesSortedBy(Sort.by("data")))
            .isInstanceOf(BusinessException.class);
    }
}
