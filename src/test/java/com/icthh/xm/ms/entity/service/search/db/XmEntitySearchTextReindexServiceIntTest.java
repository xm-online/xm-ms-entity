package com.icthh.xm.ms.entity.service.search.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.icthh.xm.ms.entity.AbstractPostgresIntTest;
import com.icthh.xm.ms.entity.domain.XmEntity;
import com.icthh.xm.ms.entity.web.rest.XmEntityDbSearchResource;
import org.springframework.security.test.context.support.WithMockUser;
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
    private XmEntityDbSearchResource resource;
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
    @WithMockUser(authorities = "SUPER-ADMIN")
    public void reindexEndpointReturnsProcessedCount() {
        assertThat(searchText()).isNull();

        var response = resource.reindex("ORDER");

        assertThat(response.getBody()).containsKey("processed");
        assertThat(response.getBody().get("processed")).isGreaterThanOrEqualTo(1L);
        assertThat(searchText()).isEqualTo("Legacy\n7");
    }

    @Test
    public void reindexWithoutTypeKeySkipsExplicitlyDisabledSubtype() {
        Long quietId = tx.execute(status -> {
            XmEntity quiet = newEntity("ORDER.QUIET", "Quiet", Map.of("orderNo", 3));
            em.persist(quiet);
            em.flush();
            return quiet.getId();
        });

        long withSubtypes = reindexService.reindex("ORDER");
        long enabledOnly = reindexService.reindex(null);

        assertThat(enabledOnly).isLessThan(withSubtypes);
        tx.executeWithoutResult(s -> em.createNativeQuery("delete from xm_entity where id = :id")
            .setParameter("id", quietId).executeUpdate());
    }

    @Test
    public void reindexRejectsTypeKeyThatIsNotInTheSpec() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reindexService.reindex("NO_SUCH_TYPE"))
            .isInstanceOf(com.icthh.xm.commons.exceptions.BusinessException.class)
            .hasMessageContaining("NO_SUCH_TYPE");
    }

    @Test
    public void reindexFillsMissingSearchText() {
        assertThat(searchText()).isNull();

        long processed = reindexService.reindex("ORDER");

        assertThat(processed).isGreaterThanOrEqualTo(1);
        assertThat(searchText()).isEqualTo("Legacy\n7");
    }
}
