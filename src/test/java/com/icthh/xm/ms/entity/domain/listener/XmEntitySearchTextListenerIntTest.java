package com.icthh.xm.ms.entity.domain.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.repository.XmEntityRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class XmEntitySearchTextListenerIntTest extends AbstractPostgresIntTest {

    @Autowired
    private XmEntityRepository repository;
    @Autowired
    private EntityManager em;

    @BeforeEach
    public void spec() {
        pushDbSearchSpec();
    }

    private String storedSearchText(Long id) {
        em.flush();
        em.clear();
        return (String) em.createNativeQuery("select search_text from xm_entity where id = :id")
            .setParameter("id", id).getSingleResult();
    }

    @Test
    public void fillsSearchTextOnPersistForEnabledType() {
        XmEntity order = repository.save(newEntity("ORDER", "Alpha", Map.of(
            "orderNo", 42, "customer", Map.of("city", "Kyiv"), "tags", List.of("vip"))));

        assertThat(storedSearchText(order.getId())).isEqualTo("Alpha\n42\nKyiv\n[vip]");
    }

    @Test
    public void leavesNullForDisabledType() {
        XmEntity silent = repository.save(newEntity("SILENT", "Quiet", Map.of("orderNo", 1)));
        assertThat(storedSearchText(silent.getId())).isNull();
    }

    @Test
    public void updatesSearchTextOnUpdate() {
        XmEntity order = repository.save(newEntity("ORDER", "Alpha", Map.of("orderNo", 1)));
        em.flush();

        order.setName("Beta");
        order.setData(Map.of("orderNo", 2));
        repository.save(order);

        assertThat(storedSearchText(order.getId())).isEqualTo("Beta\n2");
    }

    @Test
    public void indexExistsWhenExtensionAvailable() {
        Number extensions = (Number) em.createNativeQuery("select count(*) from pg_extension where extname = 'pg_trgm'").getSingleResult();
        Number indexes = (Number) em.createNativeQuery("select count(*) from pg_indexes where indexname = 'idx_xm_entity_search_text_trgm' and schemaname = current_schema()").getSingleResult();
        assertThat(indexes.intValue()).isEqualTo(extensions.intValue());
    }
}
