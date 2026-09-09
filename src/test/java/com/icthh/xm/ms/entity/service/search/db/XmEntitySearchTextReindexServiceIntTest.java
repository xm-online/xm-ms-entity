package com.icthh.xm.ms.entity.service.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

public class XmEntitySearchTextReindexServiceIntTest extends AbstractPostgresIntTest {

    @Autowired
    private XmEntitySearchTextReindexService reindexService;
    @Autowired
    private TransactionTemplate tx;
    @Autowired
    private EntityManager em;

    private Long id;

    @BeforeEach
    public void seedWithoutSearchText() {
        pushDbSearchSpec();
        id = tx.execute(status -> {
            XmEntity e = newEntity("ORDER", "Legacy", Map.of("orderNo", 7));
            em.persist(e);
            em.flush();
            // simulate a row created before search_text existed
            em.createNativeQuery("update xm_entity set search_text = null where id = :id")
                .setParameter("id", e.getId())
                .executeUpdate();
            return e.getId();
        });
    }

    @AfterEach
    public void cleanup() {
        tx.executeWithoutResult(s -> em.createNativeQuery("delete from xm_entity where id = :id")
            .setParameter("id", id)
            .executeUpdate());
    }

    private String searchText() {
        return tx.execute(s -> (String) em.createNativeQuery("select search_text from xm_entity where id = :id")
            .setParameter("id", id)
            .getSingleResult());
    }

    @Test
    public void reindexFillsMissingSearchText() {
        assertThat(searchText()).isNull();

        long processed = reindexService.reindex("ORDER");

        assertThat(processed).isGreaterThanOrEqualTo(1);
        assertThat(searchText()).isEqualTo("Legacy\n7");
    }
}
